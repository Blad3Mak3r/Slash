package tv.blademaker.slash.internal

import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.annotations.*
import tv.blademaker.slash.context.GuildSlashCommandContext
import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.exceptions.InteractionTargetMismatch
import tv.blademaker.slash.ratelimit.RateLimit
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.time.Duration

class SlashCommandHandler(
    override val parent: BaseSlashCommand,
    override val function: KFunction<*>
) : Handler {

    private val annotation: OnSlashCommand = function.findAnnotation()!!
    val rateLimit: RateLimit? = function.findAnnotation()
    val permissions: Permissions? = function.findAnnotation()

    override val path = buildString {
        append(parent.commandName)
        if (annotation.group.isNotBlank()) append("/${annotation.group}")
        if (annotation.name.isNotBlank()) append("/${annotation.name}")
    }

    val target = annotation.target

    private val options: List<FunctionParameter> = buildHandlerParameters(parent, function, annotation.target)

    private fun checkTarget(ctx: SlashCommandContext) {
        val result = when (annotation.target) {
            InteractionTarget.ALL -> true
            InteractionTarget.GUILD -> ctx.event.isFromGuild
            InteractionTarget.DM -> !ctx.event.isFromGuild
        }

        if (!result) throw InteractionTargetMismatch(ctx, path, annotation.target)
    }

    suspend fun execute(ctx: SlashCommandContext, timeout: Duration) {
        checkTarget(ctx)
        withTimeout(timeout) {
            function.callSuspend(parent, ctx, *options.map { it.compile(ctx) }.toTypedArray())
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger("InteractionHandler")

        private fun printName(command: BaseSlashCommand, function: KFunction<*>) : String {
            return "${command::class.simpleName}#${function.name}()"
        }

        private fun buildHandlerParameters(command: BaseSlashCommand, function: KFunction<*>, target: InteractionTarget): List<FunctionParameter> {
            check(!function.parameters.any { it.isVararg }) {
                "SlashCommand cannot have varargs parameters: ${function.name}"
            }

            val parametersList = mutableListOf<FunctionParameter>()

            check(function.parameters.size >= 2) {
                "Not enough parameters: ${command.commandName} -> ${function.name}"
            }

            val contextClassifier = function.parameters[1].type.classifier

            val classFunctionName = printName(command, function)

            check(contextClassifier !is SlashCommandContext) {
                "The first parameter of a SlashCommand have to be SlashCommandContext -> $classFunctionName : ${function.parameters.first().type.classifier}"
            }

            when (target) {
                InteractionTarget.ALL, InteractionTarget.DM -> {
                    check(contextClassifier != GuildSlashCommandContext::class) {
                        "Do not use GuildSlashCommandContext with a non-guild InteractionTarget, use SlashCommandContext instead -> $classFunctionName"
                    }
                }
                InteractionTarget.GUILD -> {
                    if (contextClassifier != GuildSlashCommandContext::class) {
                        log.warn("You are not using GuildSlashCommandContext on a guild InteractionTarget -> $classFunctionName")
                    }
                }
            }

            if (function.parameters.size <= 2) return parametersList

            val validParams = function.parameters.subList(2, function.parameters.size)

            for (param in validParams) {
                val name = param.findAnnotation<OptionName>()?.value ?: param.name!!
                val kType = param.type
                check(ValidOptionTypes.isValidType(kType.classifier)) {
                    "${kType.classifier} is not a valid type for SlashCommand option: ${function.name}"
                }

                parametersList.add(FunctionParameter(command, function, name, kType))
            }

            return parametersList
        }
    }
}