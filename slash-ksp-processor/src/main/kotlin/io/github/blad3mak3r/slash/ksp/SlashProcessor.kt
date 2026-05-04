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

class SlashProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(ANN_APPLICATION_COMMAND)
            .filterIsInstance<KSClassDeclaration>()

        val deferred = symbols.filter { !it.validate() }.toList()

        symbols.filter { it.validate() }.forEach { classDecl ->
            val model = resolveCommandModel(classDecl) ?: return@forEach
            RegistrarGenerator.generate(codeGenerator, model)
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

        val commandName = appAnn.arguments.first { it.name?.asString() == "name" }.value as String
        val commandType = (appAnn.arguments.firstOrNull { it.name?.asString() == "type" }?.value
            ?.let { (it as? KSType)?.declaration?.simpleName?.asString() } ?: "SLASH")

        val classRequire  = resolveRequire(classDecl.annotations)
        val classPerms    = resolvePermissions(classDecl.annotations)
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
                val pattern = ann.arguments.first { it.name?.asString() == "pattern" }.value as String
                ButtonHandlerModel(fn, pattern)
            }.toList()

        val modalHandlers = functions
            .filter { fn -> fn.annotations.any { it.fqn == ANN_ON_MODAL } }
            .map { fn ->
                val ann = fn.annotations.first { it.fqn == ANN_ON_MODAL }
                val pattern = ann.arguments.first { it.name?.asString() == "pattern" }.value as String
                ModalHandlerModel(fn, pattern)
            }.toList()

        val userHandler = functions
            .firstOrNull { fn -> fn.annotations.any { it.fqn == ANN_ON_USER } }
            ?.let { fn -> UserHandlerModel(fn, resolveRequire(fn.annotations)) }

        val messageHandler = functions
            .firstOrNull { fn -> fn.annotations.any { it.fqn == ANN_ON_MESSAGE } }
            ?.let { fn -> MessageHandlerModel(fn, resolveRequire(fn.annotations)) }

        return CommandModel(
            classDecl      = classDecl,
            commandName    = commandName,
            commandType    = commandType,
            classRequire   = classRequire,
            classPermissions = classPerms,
            classRateLimit = classRateLimit,
            slashHandlers  = slashHandlers,
            autoCompleteHandlers = autoCompleteHandlers,
            buttonHandlers = buttonHandlers,
            modalHandlers  = modalHandlers,
            userHandler    = userHandler,
            messageHandler = messageHandler
        )
    }

    private fun resolveSlashHandler(commandName: String, fn: KSFunctionDeclaration): SlashHandlerModel? {
        val ann = fn.annotations.first { it.fqn == ANN_ON_SLASH }
        val group          = ann.arg("group") as? String ?: ""
        val name           = ann.arg("name") as? String ?: ""
        val target         = (ann.arg("target") as? KSType)?.declaration?.simpleName?.asString() ?: "ALL"
        val supportDetached = ann.arg("supportDetached") as? Boolean ?: false

        // Skip first param (ctx) and resolve remaining as option parameters
        val params = fn.parameters.drop(1).mapNotNull { param ->
            val optionName = param.annotations
                .firstOrNull { it.fqn == ANN_OPTION_NAME }
                ?.arguments?.first()?.value as? String
                ?: param.name?.asString()
                ?: run {
                    logger.error("Cannot resolve parameter name in ${fn.simpleName.asString()}", param)
                    return@mapNotNull null
                }

            val typeName = param.type.resolve().declaration.simpleName.asString()
            if (!TypeMapping.isSupported(typeName)) {
                logger.error(
                    "Unsupported option type '$typeName' in ${fn.simpleName.asString()}. " +
                    "Supported: String, Long, Int, Boolean, Double, Float, Member, User, Role, Message.Attachment, channels.",
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
        val ann = fn.annotations.first { it.fqn == ANN_ON_AUTOCOMPLETE }
        val group      = ann.arg("group") as? String ?: ""
        val name       = ann.arg("name") as? String ?: ""
        val optionName = ann.arg("option") as? String ?: run {
            logger.error("@OnAutoComplete on ${fn.simpleName.asString()} missing 'option'", fn)
            return null
        }
        return AutoCompleteHandlerModel(fn, group, name, optionName)
    }

    // ── Annotation helpers ────────────────────────────────────────────────────

    private fun resolveRequire(annotations: Sequence<KSAnnotation>): List<String> {
        val ann = annotations.firstOrNull { it.fqn == ANN_REQUIRE } ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        val klasses = ann.arguments.firstOrNull()?.value as? List<KSType> ?: return emptyList()
        return klasses.mapNotNull { it.declaration.qualifiedName?.asString() }
    }

    private fun resolvePermissions(annotations: Sequence<KSAnnotation>): PermissionsModel? {
        val ann = annotations.firstOrNull { it.fqn == ANN_PERMISSIONS } ?: return null
        @Suppress("UNCHECKED_CAST")
        val perms = (ann.arg("value") as? List<KSType>)
            ?.mapNotNull { it.declaration.simpleName.asString() }
            ?: return null
        val target = (ann.arg("target") as? KSType)?.declaration?.simpleName?.asString() ?: "USER"
        return PermissionsModel(perms, target)
    }

    private fun resolveRateLimit(annotations: Sequence<KSAnnotation>): RateLimitModel? {
        val ann = annotations.firstOrNull { it.fqn == ANN_RATE_LIMIT } ?: return null
        val limit  = (ann.arg("limit") as? Int) ?: return null
        val period = when (val p = ann.arg("period")) {
            is Long -> p
            is Int  -> p.toLong()
            else    -> return null
        }
        return RateLimitModel(limit, period)
    }

    // ── Extension helpers ─────────────────────────────────────────────────────

    private val KSAnnotation.fqn: String
        get() = annotationType.resolve().declaration.qualifiedName?.asString() ?: ""

    private fun KSAnnotation.arg(name: String): Any? =
        arguments.firstOrNull { it.name?.asString() == name }?.value
}
