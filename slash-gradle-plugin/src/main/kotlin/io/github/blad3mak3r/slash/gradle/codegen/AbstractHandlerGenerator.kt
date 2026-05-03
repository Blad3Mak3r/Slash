package io.github.blad3mak3r.slash.gradle.codegen

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

/**
 * Generates an `Abstract*CommandHandler` file for a single [CommandDef], loaded via reflection
 * from the slashDefs compilation output.
 *
 * All access to the CommandDef and its children is via standard Java reflection so that this code
 * compiles and runs even when slash-dsl is only on the URLClassLoader classpath.
 */
object AbstractHandlerGenerator {

    private val ABSTRACT_COMMAND_HANDLER =
        ClassName("io.github.blad3mak3r.slash", "AbstractCommandHandler")
    private val SLASH_COMMAND_DATA =
        ClassName("net.dv8tion.jda.api.interactions.commands.build", "SlashCommandData")
    private val COMMANDS =
        ClassName("net.dv8tion.jda.api.interactions.commands", "Commands")
    private val SUBCOMMAND_DATA =
        ClassName("net.dv8tion.jda.api.interactions.commands.build", "SubcommandData")
    private val SUBCOMMAND_GROUP_DATA =
        ClassName("net.dv8tion.jda.api.interactions.commands.build", "SubcommandGroupData")
    private val OPTION_DATA =
        ClassName("net.dv8tion.jda.api.interactions.commands.build", "OptionData")
    private val OPTION_TYPE =
        ClassName("net.dv8tion.jda.api.interactions.commands", "OptionType")
    private val SLASH_CMD_CTX =
        ClassName("io.github.blad3mak3r.slash.context", "SlashCommandContext")
    private val GUILD_SLASH_CMD_CTX =
        ClassName("io.github.blad3mak3r.slash.context", "GuildSlashCommandContext")
    private val BUTTON_CTX =
        ClassName("io.github.blad3mak3r.slash.context", "ButtonContext")
    private val MODAL_CTX =
        ClassName("io.github.blad3mak3r.slash.context", "ModalContext")
    private val AUTO_COMPLETE_CTX =
        ClassName("io.github.blad3mak3r.slash.context", "AutoCompleteContext")

    /** Reflective helpers to read a CommandDef (or sub-types) loaded from a foreign ClassLoader. */
    private fun Any.prop(name: String): Any? = javaClass.getMethod(name).invoke(this)
    private fun Any.str(name: String) = prop(name) as String
    private fun Any.bool(name: String) = prop(name) as Boolean
    @Suppress("UNCHECKED_CAST")
    private fun Any.list(name: String) = prop(name) as List<*>
    private fun Any.enumName(name: String) = (prop(name) as Enum<*>).name

    // ── Public entry point ────────────────────────────────────────────────────

    fun generate(commandDef: Any): FileSpec {
        val cmdName = commandDef.str("getName")
        val description = commandDef.str("getDescription")
        val target = commandDef.enumName("getTarget")        // GUILD | DM | ALL
        val subcommands = commandDef.list("getSubcommands")
        val subcommandGroups = commandDef.list("getSubcommandGroups")
        val topLevelOptions = commandDef.list("getOptions")
        val buttons = commandDef.list("getButtons")
        val modals = commandDef.list("getModals")

        val className = "Abstract${cmdName.toPascalCase()}CommandHandler"
        val ctxType = if (target == "GUILD") GUILD_SLASH_CMD_CTX else SLASH_CMD_CTX

        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.ABSTRACT)
            .superclass(ABSTRACT_COMMAND_HANDLER)
            .addFunction(buildGetCommandName(cmdName))
            .addFunction(buildBuildCommandData(cmdName, description, target, subcommands, subcommandGroups, topLevelOptions))
            .addFunction(buildDispatch(cmdName, target, subcommands, topLevelOptions, ctxType))

        // dispatchButton
        if (buttons.isNotEmpty()) {
            classBuilder.addFunction(buildDispatchButton(cmdName, buttons))
        }
        // dispatchModal
        if (modals.isNotEmpty()) {
            classBuilder.addFunction(buildDispatchModal(cmdName, modals))
        }

        // Abstract handler methods for each subcommand
        for (sub in subcommands) {
            sub!!
            val subName = sub.str("getName")
            val options = sub.list("getOptions")
            classBuilder.addFunction(buildAbstractSubcommandFun(subName, options, ctxType))
        }

        // If there are top-level options (no subcommands), add a single abstract handler
        if (subcommands.isEmpty() && subcommandGroups.isEmpty() && topLevelOptions.isNotEmpty()) {
            classBuilder.addFunction(buildAbstractRootFun(cmdName, topLevelOptions, ctxType))
        }

        // Abstract button handlers
        for (btn in buttons) {
            btn!!
            val pattern = btn.str("getPattern")
            classBuilder.addFunction(
                FunSpec.builder("on${pattern.toHandlerSuffix()}Button")
                    .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
                    .addParameter("ctx", BUTTON_CTX)
                    .build()
            )
        }

        // Abstract modal handlers
        for (modal in modals) {
            modal!!
            val pattern = modal.str("getPattern")
            classBuilder.addFunction(
                FunSpec.builder("on${pattern.toHandlerSuffix()}Modal")
                    .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
                    .addParameter("ctx", MODAL_CTX)
                    .build()
            )
        }

        // Companion with regex patterns
        val companions = mutableListOf<PropertySpec>()
        for (btn in buttons) {
            btn!!
            val pattern = btn.str("getPattern")
            companions += PropertySpec.builder(
                "${pattern.toConstName()}_BTN_REGEX", Regex::class
            )
                .initializer("Regex(%S)", pattern)
                .build()
        }
        for (modal in modals) {
            modal!!
            val pattern = modal.str("getPattern")
            companions += PropertySpec.builder(
                "${pattern.toConstName()}_MODAL_REGEX", Regex::class
            )
                .initializer("Regex(%S)", pattern)
                .build()
        }
        if (companions.isNotEmpty()) {
            classBuilder.addType(
                TypeSpec.companionObjectBuilder()
                    .addProperties(companions)
                    .build()
            )
        }

        return FileSpec.builder("io.github.blad3mak3r.slash.generated", className)
            .addType(classBuilder.build())
            .build()
    }

    // ── Private builders ──────────────────────────────────────────────────────

    private fun buildGetCommandName(name: String) =
        FunSpec.builder("getCommandName")
            .addModifiers(KModifier.OVERRIDE)
            .returns(String::class)
            .addStatement("return %S", name)
            .build()

    private fun buildBuildCommandData(
        name: String,
        description: String,
        target: String,
        subcommands: List<*>,
        subcommandGroups: List<*>,
        topLevelOptions: List<*>
    ): FunSpec {
        val code = CodeBlock.builder()
        code.addStatement(
            "val cmd = %T.slash(%S, %S)",
            COMMANDS, name, description
        )
        if (target == "GUILD") code.addStatement("cmd.setGuildOnly(true)")
        if (target == "DM") code.addStatement("cmd.setGuildOnly(false)")

        for (sub in subcommands) {
            sub!!
            code.add(buildSubcommandBlock(sub))
        }
        for (group in subcommandGroups) {
            group!!
            code.add(buildSubcommandGroupBlock(group))
        }
        for (opt in topLevelOptions) {
            opt!!
            code.add(buildOptionBlock(opt))
        }
        code.addStatement("return cmd")

        return FunSpec.builder("buildCommandData")
            .addModifiers(KModifier.OVERRIDE)
            .returns(SLASH_COMMAND_DATA)
            .addCode(code.build())
            .build()
    }

    private fun buildSubcommandBlock(sub: Any): CodeBlock {
        val subName = sub.str("getName")
        val subDesc = sub.str("getDescription")
        val options = sub.list("getOptions")
        val code = CodeBlock.builder()
        code.addStatement(
            "val sub_${subName.toVarName()} = %T(%S, %S)",
            SUBCOMMAND_DATA, subName, subDesc
        )
        for (opt in options) {
            opt!!
            code.add(buildOptionToSubBlock(opt, "sub_${subName.toVarName()}"))
        }
        code.addStatement("cmd.addSubcommands(sub_${subName.toVarName()})")
        return code.build()
    }

    private fun buildSubcommandGroupBlock(group: Any): CodeBlock {
        val groupName = group.str("getName")
        val groupDesc = group.str("getDescription")
        val subs = group.list("getSubcommands")
        val code = CodeBlock.builder()
        code.addStatement(
            "val grp_${groupName.toVarName()} = %T(%S, %S)",
            SUBCOMMAND_GROUP_DATA, groupName, groupDesc
        )
        for (sub in subs) {
            sub!!
            val subName = sub.str("getName")
            val subDesc = sub.str("getDescription")
            val opts = sub.list("getOptions")
            code.addStatement(
                "val grp_sub_${subName.toVarName()} = %T(%S, %S)",
                SUBCOMMAND_DATA, subName, subDesc
            )
            for (opt in opts) {
                opt!!
                code.add(buildOptionToSubBlock(opt, "grp_sub_${subName.toVarName()}"))
            }
            code.addStatement("grp_${groupName.toVarName()}.addSubcommands(grp_sub_${subName.toVarName()})")
        }
        code.addStatement("cmd.addSubcommandGroups(grp_${groupName.toVarName()})")
        return code.build()
    }

    private fun buildOptionBlock(opt: Any): CodeBlock {
        val optName = opt.str("getName")
        val optDesc = opt.str("getDescription")
        val typeClass = opt.prop("getType")!!        // KClass<*> loaded via classloader
        val required = opt.bool("getRequired")

        // KClass<*>.java.name is accessible without kotlin-reflect
        val javaTypeName = resolveJavaTypeName(typeClass)

        val mapping = TypeMappings.get(javaTypeName)
        return CodeBlock.of(
            "cmd.addOptions(%T(%T.%L, %S, %S, %L))\n",
            OPTION_DATA, OPTION_TYPE, mapping.optionType, optName, optDesc, required
        )
    }

    private fun buildOptionToSubBlock(opt: Any, targetVar: String): CodeBlock {
        val optName = opt.str("getName")
        val optDesc = opt.str("getDescription")
        val typeClass = opt.prop("getType")!!
        val required = opt.bool("getRequired")

        val javaTypeName = resolveJavaTypeName(typeClass)
        val mapping = TypeMappings.get(javaTypeName)
        return CodeBlock.of(
            "%L.addOptions(%T(%T.%L, %S, %S, %L))\n",
            targetVar, OPTION_DATA, OPTION_TYPE, mapping.optionType, optName, optDesc, required
        )
    }

    private fun buildDispatch(
        cmdName: String,
        target: String,
        subcommands: List<*>,
        topLevelOptions: List<*>,
        ctxType: ClassName
    ): FunSpec {
        val code = CodeBlock.builder()
        if (target == "GUILD") {
            code.addStatement("val gCtx = ctx as %T", GUILD_SLASH_CMD_CTX)
        }
        val dispatchCtx = if (target == "GUILD") "gCtx" else "ctx"

        if (subcommands.isNotEmpty()) {
            code.beginControlFlow("when (ctx.event.subcommandName)")
            for (sub in subcommands) {
                sub!!
                val subName = sub.str("getName")
                val options = sub.list("getOptions")
                code.beginControlFlow("%S ->", subName)
                val argList = buildOptionExtractions(code, options)
                code.addStatement("on${subName.toPascalCase()}(%L)", "$dispatchCtx, $argList".trim(',',' '))
                code.endControlFlow()
            }
            code.endControlFlow()
        } else if (topLevelOptions.isNotEmpty()) {
            val argList = buildOptionExtractions(code, topLevelOptions)
            code.addStatement("on${cmdName.toPascalCase()}(%L)", "$dispatchCtx, $argList".trim(',',' '))
        }

        return FunSpec.builder("dispatch")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("ctx", SLASH_CMD_CTX)
            .addCode(code.build())
            .build()
    }

    /**
     * Emits option extraction statements and returns a comma-separated arg list.
     */
    private fun buildOptionExtractions(code: CodeBlock.Builder, options: List<*>): String {
        val args = mutableListOf<String>()
        for (opt in options) {
            opt!!
            val optName = opt.str("getName")
            val required = opt.bool("getRequired")
            val typeClass = opt.prop("getType")!!
            val javaTypeName = resolveJavaTypeName(typeClass)
            val mapping = TypeMappings.get(javaTypeName)
            val varName = optName.toVarName()
            if (required) {
                code.addStatement(
                    "val %L = ctx.event.getOption(%S)!!%L",
                    varName, optName, mapping.accessor
                )
            } else {
                code.addStatement(
                    "val %L = ctx.event.getOption(%S)%L%L",
                    varName, optName,
                    if (mapping.accessor.endsWith(")")) "?" else "",
                    mapping.accessor
                )
            }
            args += varName
        }
        return args.joinToString(", ")
    }

    private fun buildDispatchButton(cmdName: String, buttons: List<*>): FunSpec {
        val code = CodeBlock.builder()
        code.addStatement("val id = ctx.componentId")
        for (btn in buttons) {
            btn!!
            val pattern = btn.str("getPattern")
            val constName = "${pattern.toConstName()}_BTN_REGEX"
            val handlerName = "on${pattern.toHandlerSuffix()}Button"
            code.beginControlFlow("if (%L.matches(id))", constName)
            code.addStatement("%L(ctx)", handlerName)
            code.addStatement("return true")
            code.endControlFlow()
        }
        code.addStatement("return false")
        return FunSpec.builder("dispatchButton")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("ctx", BUTTON_CTX)
            .returns(Boolean::class)
            .addCode(code.build())
            .build()
    }

    private fun buildDispatchModal(cmdName: String, modals: List<*>): FunSpec {
        val code = CodeBlock.builder()
        code.addStatement("val id = ctx.modalId")
        for (modal in modals) {
            modal!!
            val pattern = modal.str("getPattern")
            val constName = "${pattern.toConstName()}_MODAL_REGEX"
            val handlerName = "on${pattern.toHandlerSuffix()}Modal"
            code.beginControlFlow("if (%L.matches(id))", constName)
            code.addStatement("%L(ctx)", handlerName)
            code.addStatement("return true")
            code.endControlFlow()
        }
        code.addStatement("return false")
        return FunSpec.builder("dispatchModal")
            .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
            .addParameter("ctx", MODAL_CTX)
            .returns(Boolean::class)
            .addCode(code.build())
            .build()
    }

    private fun buildAbstractSubcommandFun(subName: String, options: List<*>, ctxType: ClassName): FunSpec {
        val builder = FunSpec.builder("on${subName.toPascalCase()}")
            .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
            .addParameter("ctx", ctxType)
        for (opt in options) {
            opt!!
            val optName = opt.str("getName")
            val required = opt.bool("getRequired")
            val typeClass = opt.prop("getType")!!
            val javaTypeName = resolveJavaTypeName(typeClass)
            val mapping = TypeMappings.get(javaTypeName)
            val kotlinType = mapping.returnType.toKotlinTypeName()
            builder.addParameter(
                ParameterSpec.builder(optName.toVarName(), kotlinType.copy(nullable = !required)).build()
            )
        }
        return builder.build()
    }

    private fun buildAbstractRootFun(cmdName: String, options: List<*>, ctxType: ClassName): FunSpec {
        val builder = FunSpec.builder("on${cmdName.toPascalCase()}")
            .addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
            .addParameter("ctx", ctxType)
        for (opt in options) {
            opt!!
            val optName = opt.str("getName")
            val required = opt.bool("getRequired")
            val typeClass = opt.prop("getType")!!
            val javaTypeName = resolveJavaTypeName(typeClass)
            val mapping = TypeMappings.get(javaTypeName)
            val kotlinType = mapping.returnType.toKotlinTypeName()
            builder.addParameter(
                ParameterSpec.builder(optName.toVarName(), kotlinType.copy(nullable = !required)).build()
            )
        }
        return builder.build()
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

    /**
     * Resolves a KClass<*> instance loaded from a foreign ClassLoader to a Java class name.
     *
     * When KClass is loaded from a URLClassLoader it is an instance of
     * `org.jetbrains.kotlin.builtins.KotlinType` or the JVM backing `Class<*>`.
     * The reliable path: KClass delegates to a Java Class; call `getJava()` via reflection.
     */
    private fun resolveJavaTypeName(kclassInstance: Any): String {
        return when (kclassInstance) {
            is Class<*> -> kclassInstance.name
            else -> {
                // Kotlin KClass loaded via foreign classloader — access .java property
                try {
                    val javaGetter = kclassInstance.javaClass.getMethod("getJava")
                    (javaGetter.invoke(kclassInstance) as Class<*>).name
                } catch (_: Exception) {
                    // Fallback: toString looks like "class java.lang.String"
                    kclassInstance.toString()
                        .removePrefix("class ")
                        .removePrefix("interface ")
                        .trim()
                }
            }
        }
    }

    // ── String helpers ────────────────────────────────────────────────────────

    private fun String.toPascalCase(): String =
        split("-", "_", " ").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }

    private fun String.toVarName(): String = toCamelCase().let {
        // avoid Kotlin keywords
        if (it in setOf("val", "var", "fun", "class", "object", "in", "is", "as")) "`$it`" else it
    }

    private fun String.toCamelCase(): String =
        split("-", "_", " ").mapIndexed { i, s ->
            if (i == 0) s.lowercase() else s.replaceFirstChar { c -> c.uppercase() }
        }.joinToString("")

    /** Derives a suffix for handler method names from a regex pattern, e.g. "ban-confirm-[a-z0-9]+" → "BanConfirm" */
    private fun String.toHandlerSuffix(): String =
        replace(Regex("[^a-zA-Z0-9 _-]"), "")   // strip regex chars
            .trim('-', '_', ' ')
            .toPascalCase()
            .ifEmpty { "Unknown" }

    /** Derives a SCREAMING_SNAKE constant name from a regex pattern */
    private fun String.toConstName(): String =
        replace(Regex("[^a-zA-Z0-9]"), "_")
            .trim('_')
            .uppercase()
            .replace(Regex("_+"), "_")

    /** Converts a Java class name to a KotlinPoet TypeName */
    private fun String.toKotlinTypeName(): TypeName = when (this) {
        "java.lang.String", "kotlin.String" -> STRING
        "java.lang.Integer", "int", "kotlin.Int" -> INT
        "java.lang.Long", "long", "kotlin.Long" -> LONG
        "java.lang.Double", "double", "kotlin.Double" -> DOUBLE
        "java.lang.Boolean", "boolean", "kotlin.Boolean" -> BOOLEAN
        else -> ClassName.bestGuess(this)
    }
}
