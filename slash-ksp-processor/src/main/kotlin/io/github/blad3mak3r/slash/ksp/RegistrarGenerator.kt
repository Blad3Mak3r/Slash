package io.github.blad3mak3r.slash.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

// Fully-qualified names used in the generated code (strings to avoid classpath deps)
private const val REGISTRY_PKG    = "io.github.blad3mak3r.slash.registry"
private const val ANNOTATIONS_PKG = "io.github.blad3mak3r.slash.annotations"
private const val CONTEXT_PKG     = "io.github.blad3mak3r.slash.context"

private val PRECONDITION_PROVIDER_TYPE = ClassName(REGISTRY_PKG, "PreconditionProvider")

object RegistrarGenerator {

    /**
     * Generates the `XxxRegistrar` source file and returns its fully-qualified
     * class name so the caller can accumulate all names and write the
     * ServiceLoader manifest file once (avoids FileAlreadyExistsException when
     * multiple @ApplicationCommand classes are present in the same compilation).
     */
    fun generate(codeGenerator: CodeGenerator, model: CommandModel): String {
        val classDecl   = model.classDecl
        val pkg         = classDecl.packageName.asString()
        val simpleName  = classDecl.simpleName.asString()
        val registrarName = "${simpleName}Registrar"

        val commandRegistrarType = ClassName(REGISTRY_PKG, "CommandRegistrar")
        val handlerRegistryType  = ClassName(REGISTRY_PKG, "HandlerRegistry")

        val instanceProp = PropertySpec.builder("_instance", classDecl.toClassName(), KModifier.PRIVATE)
            .initializer("%T()", classDecl.toClassName())
            .build()

        // Collect all unique precondition types needed
        val allPreFqns: List<String> = buildSet<String> {
            addAll(model.classRequire)
            model.slashHandlers.forEach { addAll(it.require) }
            model.userHandler?.let { addAll(it.require) }
            model.messageHandler?.let { addAll(it.require) }
        }.toList()

        val registerFun = buildRegisterFunction(handlerRegistryType, model, allPreFqns)

        val registrarClass = TypeSpec.classBuilder(registrarName)
            .addSuperinterface(commandRegistrarType)
            .addProperty(instanceProp)
            .addFunction(registerFun)
            .build()

        val file = FileSpec.builder(pkg, registrarName)
            .addType(registrarClass)
            .build()

        // Write Kotlin source
        file.writeTo(codeGenerator, Dependencies(aggregating = false, classDecl.containingFile!!))

        return "$pkg.$registrarName"
    }

    // ── register() function body ──────────────────────────────────────────────

    private fun buildRegisterFunction(
        handlerRegistryType: TypeName,
        model: CommandModel,
        allPreFqns: List<String>
    ): FunSpec {
        val builder = FunSpec.builder("register")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("registry", handlerRegistryType)
            .addParameter("preconditionProvider", PRECONDITION_PROVIDER_TYPE)

        // Emit bindIfAbsent for each unique precondition type so zero-arg defaults
        // are registered automatically (user-supplied bindings take precedence).
        allPreFqns.forEachIndexed { i, fqn ->
            val cn = ClassName.bestGuess(fqn)
            builder.addStatement(
                "preconditionProvider.bindIfAbsent(%T::class.java) { %T() }",
                cn, cn
            )
        }

        // Resolve each precondition as a local val
        allPreFqns.forEachIndexed { i, fqn ->
            val cn = ClassName.bestGuess(fqn)
            builder.addStatement(
                "val _pre%L = preconditionProvider.get(%T::class.java)",
                i, cn
            )
        }
        when (model.commandType) {
            "MESSAGE" -> {
                model.messageHandler?.let { handler ->
                    val preconditionsCode = buildPreconditionsArray(
                        model.classRequire + handler.require, allPreFqns
                    )
                    builder.addCode(
                        "registry.registerMessageCommand(%S, preconditions = %L) { ctx ->\n",
                        model.commandName, preconditionsCode
                    )
                    builder.addStatement("  _instance.%N(ctx)", handler.function.simpleName.asString())
                    builder.addStatement("}")
                }
            }
            "USER" -> {
                model.userHandler?.let { handler ->
                    val preconditionsCode = buildPreconditionsArray(
                        model.classRequire + handler.require, allPreFqns
                    )
                    builder.addCode(
                        "registry.registerUserCommand(%S, preconditions = %L) { ctx ->\n",
                        model.commandName, preconditionsCode
                    )
                    builder.addStatement("  _instance.%N(ctx)", handler.function.simpleName.asString())
                    builder.addStatement("}")
                }
            }
            else -> { // SLASH
                for (h in model.slashHandlers) {
                    val path = buildPath(model.commandName, h.group, h.name)
                    val targetFqn = "$ANNOTATIONS_PKG.InteractionTarget.${h.target}"
                    val mergedRequire = model.classRequire + h.require
                    val preconditionsCode = buildPreconditionsArray(mergedRequire, allPreFqns)

                    val permCode = if (h.permissions != null || model.classPermissions != null) {
                        buildPermissionsCode(h.permissions ?: model.classPermissions!!)
                    } else "null"

                    val rlModel = h.rateLimit ?: model.classRateLimit
                    val rlCode = if (rlModel != null)
                        "$REGISTRY_PKG.RateLimitConfig(limit = ${rlModel.limit}, period = ${rlModel.period}L)"
                    else "null"

                    builder.addCode(
                        "registry.registerSlash(\n" +
                        "  path = %S,\n" +
                        "  target = $targetFqn,\n" +
                        "  supportDetached = ${h.supportDetached},\n" +
                        "  permissions = $permCode,\n" +
                        "  rateLimit = $rlCode,\n" +
                        "  preconditions = $preconditionsCode\n" +
                        ") { _ctx ->\n",
                        path
                    )

                    // Cast to the correct context type
                    val ctxType = if (h.target == "GUILD") "GuildSlashCommandContext" else "SlashCommandContext"
                    if (h.target == "GUILD") {
                        builder.addStatement(
                            "  val ctx = _ctx as %T",
                            ClassName(CONTEXT_PKG, ctxType)
                        )
                    } else {
                        builder.addStatement("  val ctx = _ctx")
                    }

                    // Resolve option parameters
                    for (param in h.parameters) {
                        val accessor = TypeMapping.accessorFor(param.kotlinType, param.nullable) ?: continue
                        if (param.nullable) {
                            builder.addStatement(
                                "  val %N = ctx.getOption(%S)?.%L",
                                param.optionName, param.optionName, accessor
                            )
                        } else {
                            builder.addStatement(
                                "  val %N = ctx.getOption(%S)!!.%L",
                                param.optionName, param.optionName, accessor
                            )
                        }
                    }

                    val paramList = h.parameters.joinToString(", ") { it.optionName }
                    val paramSuffix = if (paramList.isNotBlank()) ", $paramList" else ""
                    builder.addStatement("  _instance.%N(ctx$paramSuffix)", h.function.simpleName.asString())
                    builder.addStatement("}")
                }

                for (ac in model.autoCompleteHandlers) {
                    val path = buildPath(model.commandName, ac.group, ac.name)
                    builder.addCode(
                        "registry.registerAutoComplete(path = %S, optionName = %S) { ctx ->\n",
                        path, ac.optionName
                    )

                    // Resolve extra option parameters (e.g. the focused option value)
                    for (param in ac.parameters) {
                        val accessor = TypeMapping.accessorFor(param.kotlinType, param.nullable) ?: continue
                        if (param.nullable) {
                            builder.addStatement(
                                "  val %N = ctx.getOption(%S)?.%L",
                                param.optionName, param.optionName, accessor
                            )
                        } else {
                            builder.addStatement(
                                "  val %N = ctx.getOption(%S)!!.%L",
                                param.optionName, param.optionName, accessor
                            )
                        }
                    }

                    val paramList = ac.parameters.joinToString(", ") { it.optionName }
                    val paramSuffix = if (paramList.isNotBlank()) ", $paramList" else ""
                    builder.addStatement("  _instance.%N(ctx$paramSuffix)", ac.function.simpleName.asString())
                    builder.addStatement("}")
                }
            }
        }

        for (btn in model.buttonHandlers) {
            builder.addCode("registry.registerButton(%S) { ctx ->\n", btn.pattern)
            builder.addStatement("  _instance.%N(ctx)", btn.function.simpleName.asString())
            builder.addStatement("}")
        }

        for (modal in model.modalHandlers) {
            builder.addCode("registry.registerModal(%S) { ctx ->\n", modal.pattern)
            builder.addStatement("  _instance.%N(ctx)", modal.function.simpleName.asString())
            builder.addStatement("}")
        }

        return builder.build()
    }

    // ── Code-gen helpers ──────────────────────────────────────────────────────

    private fun buildPath(commandName: String, group: String, name: String): String = buildString {
        append(commandName)
        if (group.isNotBlank()) append("/$group")
        if (name.isNotBlank()) append("/$name")
    }

    /**
     * Generates `arrayOf(_pre0, _pre3)` from the merged require list.
     * If empty, generates `emptyArray()`.
     */
    private fun buildPreconditionsArray(require: List<String>, allPreFqns: List<String>): String {
        if (require.isEmpty()) return "emptyArray()"
        val refs = require.map { fqn -> "_pre${allPreFqns.indexOf(fqn)}" }
        return "arrayOf(${refs.joinToString(", ")})"
    }

    private fun buildPermissionsCode(p: PermissionsModel): String {
        val permsLiteral = p.permissions.joinToString(", ") {
            "net.dv8tion.jda.api.Permission.$it"
        }
        return "$REGISTRY_PKG.PermissionsConfig(" +
               "permissions = arrayOf($permsLiteral), " +
               "target = $ANNOTATIONS_PKG.PermissionTarget.${p.target})"
    }
}
