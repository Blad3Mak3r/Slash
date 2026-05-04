package io.github.blad3mak3r.slash.ksp

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

private const val ANN_APPLICATION_COMMAND = "io.github.blad3mak3r.slash.annotations.ApplicationCommand"
private const val ANN_ON_SLASH            = "io.github.blad3mak3r.slash.annotations.OnSlashCommand"
private const val ANN_ON_AUTOCOMPLETE     = "io.github.blad3mak3r.slash.annotations.OnAutoComplete"
private const val ANN_ON_BUTTON           = "io.github.blad3mak3r.slash.annotations.OnButton"
private const val ANN_ON_MODAL            = "io.github.blad3mak3r.slash.annotations.OnModal"
private const val ANN_ON_USER             = "io.github.blad3mak3r.slash.annotations.OnUserCommand"
private const val ANN_ON_MESSAGE          = "io.github.blad3mak3r.slash.annotations.OnMessageCommand"
private const val ANN_REQUIRE             = "io.github.blad3mak3r.slash.annotations.Require"
private const val ANN_PERMISSIONS         = "io.github.blad3mak3r.slash.annotations.Permissions"
private const val ANN_RATE_LIMIT          = "io.github.blad3mak3r.slash.annotations.RateLimit"
private const val ANN_OPTION_NAME         = "io.github.blad3mak3r.slash.annotations.OptionName"
private const val SERVICE_FILE            = "META-INF/services/io.github.blad3mak3r.slash.registry.CommandRegistrar"

class SlashProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(ANN_APPLICATION_COMMAND)
            .filterIsInstance<KSClassDeclaration>()

        val deferred = symbols.filter { !it.validate() }.toList()

        val registrarFqns = mutableListOf<String>()
        val sourceFiles   = mutableListOf<KSFile>()

        symbols.filter { it.validate() }.forEach { classDecl ->
            val model = resolveCommandModel(classDecl) ?: return@forEach
            registrarFqns += RegistrarGenerator.generate(codeGenerator, model)
            classDecl.containingFile?.let { sourceFiles += it }
        }

        // Write the ServiceLoader manifest once for all registrars in this round,
        // avoiding FileAlreadyExistsException when multiple @ApplicationCommand
        // classes are present in the same compilation unit.
        if (registrarFqns.isNotEmpty()) {
            val deps = Dependencies(aggregating = true, *sourceFiles.toTypedArray())
            codeGenerator.createNewFileByPath(deps, SERVICE_FILE, extensionName = "")
                .bufferedWriter()
                .use { w -> registrarFqns.forEach { fqn -> w.write(fqn); w.newLine() } }
        }

        return deferred
    }

    // ── Model resolution ──────────────────────────────────────────────────────

    private fun resolveCommandModel(classDecl: KSClassDeclaration): CommandModel? {
        val appAnn = classDecl.annotations.firstOrNull {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == ANN_APPLICATION_COMMAND
        } ?: run {
            logger.error("Missing @ApplicationCommand on ${classDecl.simpleName.asString()}", classDecl)
            return null
        }

        val commandName = appAnn.arg("name") as? String ?: run {
            logger.error("@ApplicationCommand on ${classDecl.simpleName.asString()} is missing 'name'", classDecl)
            return null
        }
        val commandType = appAnn.arg("type")?.enumEntryName() ?: "SLASH"

        val classRequire   = resolveRequire(classDecl.annotations)
        val classPerms     = resolvePermissions(classDecl.annotations)
        val classRateLimit = resolveRateLimit(classDecl.annotations)

        val functions = classDecl.getDeclaredFunctions()

        val slashHandlers = functions
            .filter { fn -> fn.annotations.any { it.fqn == ANN_ON_SLASH } }
            .mapNotNull { fn -> resolveSlashHandler(commandName, fn) }
            .toList()

        val autoCompleteHandlers = functions
            .filter { fn -> fn.annotations.any { it.fqn == ANN_ON_AUTOCOMPLETE } }
            .mapNotNull { fn -> resolveAutoCompleteHandler(commandName, fn) }
            .toList()

        val buttonHandlers = functions
            .filter { fn -> fn.annotations.any { it.fqn == ANN_ON_BUTTON } }
            .map { fn ->
                val ann = fn.annotations.first { it.fqn == ANN_ON_BUTTON }
                val pattern = ann.arg("pattern") as? String ?: ""
                ButtonHandlerModel(fn, pattern)
            }.toList()

        val modalHandlers = functions
            .filter { fn -> fn.annotations.any { it.fqn == ANN_ON_MODAL } }
            .map { fn ->
                val ann = fn.annotations.first { it.fqn == ANN_ON_MODAL }
                val pattern = ann.arg("pattern") as? String ?: ""
                ModalHandlerModel(fn, pattern)
            }.toList()

        val userHandler = functions
            .firstOrNull { fn -> fn.annotations.any { it.fqn == ANN_ON_USER } }
            ?.let { fn -> UserHandlerModel(fn, resolveRequire(fn.annotations)) }

        val messageHandler = functions
            .firstOrNull { fn -> fn.annotations.any { it.fqn == ANN_ON_MESSAGE } }
            ?.let { fn -> MessageHandlerModel(fn, resolveRequire(fn.annotations)) }

        return CommandModel(
            classDecl            = classDecl,
            commandName          = commandName,
            commandType          = commandType,
            classRequire         = classRequire,
            classPermissions     = classPerms,
            classRateLimit       = classRateLimit,
            slashHandlers        = slashHandlers,
            autoCompleteHandlers = autoCompleteHandlers,
            buttonHandlers       = buttonHandlers,
            modalHandlers        = modalHandlers,
            userHandler          = userHandler,
            messageHandler       = messageHandler
        )
    }

    private fun resolveSlashHandler(commandName: String, fn: KSFunctionDeclaration): SlashHandlerModel? {
        val ann             = fn.annotations.first { it.fqn == ANN_ON_SLASH }
        val group           = ann.arg("group") as? String ?: ""
        val name            = ann.arg("name") as? String ?: ""
        val target          = ann.arg("target")?.enumEntryName() ?: "ALL"
        val supportDetached = ann.arg("supportDetached") as? Boolean ?: false

        // Drop first param (ctx) and resolve the rest as Discord options
        val params = fn.parameters.drop(1).mapNotNull { param ->
            val optionName = param.annotations
                .firstOrNull { it.fqn == ANN_OPTION_NAME }
                ?.arg("value") as? String
                ?: param.name?.asString()
                ?: run {
                    logger.error("Cannot resolve parameter name in ${fn.simpleName.asString()}", param)
                    return@mapNotNull null
                }

            val typeName = param.type.resolve().declaration.simpleName.asString()
            if (!TypeMapping.isSupported(typeName)) {
                logger.error(
                    "Unsupported option type '$typeName' in ${fn.simpleName.asString()}. " +
                    "Supported: String, Long, Int, Boolean, Double, Float, Member, User, Role, Attachment, channels.",
                    param
                )
                return null
            }

            ParameterModel(
                optionName = optionName,
                kotlinType = typeName,
                nullable   = param.type.resolve().isMarkedNullable
            )
        }

        return SlashHandlerModel(
            function        = fn,
            group           = group,
            name            = name,
            target          = target,
            supportDetached = supportDetached,
            require         = resolveRequire(fn.annotations),
            permissions     = resolvePermissions(fn.annotations),
            rateLimit       = resolveRateLimit(fn.annotations),
            parameters      = params
        )
    }

    private fun resolveAutoCompleteHandler(commandName: String, fn: KSFunctionDeclaration): AutoCompleteHandlerModel? {
        val ann        = fn.annotations.first { it.fqn == ANN_ON_AUTOCOMPLETE }
        val group      = ann.arg("group") as? String ?: ""
        val name       = ann.arg("name") as? String ?: ""
        val optionName = ann.arg("option") as? String ?: run {
            logger.error("@OnAutoComplete on ${fn.simpleName.asString()} is missing 'option'", fn)
            return null
        }
        return AutoCompleteHandlerModel(fn, group, name, optionName)
    }

    // ── Annotation helpers ────────────────────────────────────────────────────

    private fun resolveRequire(annotations: Sequence<KSAnnotation>): List<String> {
        val ann = annotations.firstOrNull { it.fqn == ANN_REQUIRE } ?: return emptyList()
        // KClass<> args are still KSType in both KSP1 and KSP2
        @Suppress("UNCHECKED_CAST")
        val klasses = ann.arg("value") as? List<KSType> ?: return emptyList()
        return klasses.mapNotNull { it.declaration.qualifiedName?.asString() }
    }

    private fun resolvePermissions(annotations: Sequence<KSAnnotation>): PermissionsModel? {
        val ann = annotations.firstOrNull { it.fqn == ANN_PERMISSIONS } ?: return null
        // In KSP2, enum entries in arrays come as KSClassDeclaration; in KSP1 as KSType.
        val perms = (ann.arg("value") as? List<*>)
            ?.mapNotNull { it?.enumEntryName() }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        val target = ann.arg("target")?.enumEntryName() ?: "USER"
        return PermissionsModel(perms, target)
    }

    private fun resolveRateLimit(annotations: Sequence<KSAnnotation>): RateLimitModel? {
        val ann    = annotations.firstOrNull { it.fqn == ANN_RATE_LIMIT } ?: return null
        val limit  = ann.arg("limit") as? Int ?: return null
        val period = when (val p = ann.arg("period")) {
            is Long -> p
            is Int  -> p.toLong()
            else    -> return null
        }
        return RateLimitModel(limit, period)
    }

    // ── Extension helpers ─────────────────────────────────────────────────────

    /** Fully-qualified annotation name, used for identity checks. */
    private val KSAnnotation.fqn: String
        get() = annotationType.resolve().declaration.qualifiedName?.asString() ?: ""

    /**
     * Looks up an annotation argument by name, checking both explicitly-provided
     * [KSAnnotation.arguments] and [KSAnnotation.defaultArguments].
     *
     * KSP (both KSP1 and KSP2) only puts explicitly-provided values in [arguments];
     * parameters that were not written at the call site live in [defaultArguments].
     */
    private fun KSAnnotation.arg(name: String): Any? =
        (arguments + defaultArguments).firstOrNull { it.name?.asString() == name }?.value

    /**
     * Resolves the simple name of an enum entry regardless of KSP version:
     * - KSP1 / legacy: enum entries in annotation args arrive as [KSType].
     * - KSP2 / K2 AA:  enum entries arrive as [KSClassDeclaration]
     *   (specifically KSClassDeclarationEnumEntryImpl).
     */
    private fun Any.enumEntryName(): String? = when (this) {
        is KSType -> declaration.simpleName.asString()
        is KSClassDeclaration -> simpleName.asString()
        else -> null
    }
}
