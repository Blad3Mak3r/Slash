package tv.blademaker.slash.internal

import org.slf4j.LoggerFactory
import tv.blademaker.slash.SlashUtils
import tv.blademaker.slash.annotations.*
import tv.blademaker.slash.context.GuildSlashCommandContext
import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.exceptions.InteractionTargetMismatch
import tv.blademaker.slash.ratelimit.RateLimit
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters

class SlashCommandHandler(
    override val annotation: OnSlashCommand,
    override val function: KFunction<*>
) : Handler<OnSlashCommand, SlashCommandContext> {

    constructor(entry: Map.Entry<OnSlashCommand, KFunction<*>>) : this(
        annotation = entry.key,
        function = entry.value
    )

    val rateLimit: RateLimit? = function.findAnnotation()
    val permissions: Permissions? = function.findAnnotation()

    val target = annotation.target

    private val options: List<FunctionParameter> = buildHandlerParameters(this, annotation.target)

    private fun checkTarget(ctx: SlashCommandContext) {
        val result = when (annotation.target) {
            InteractionTarget.ALL -> true
            InteractionTarget.GUILD -> ctx.event.isFromGuild
            InteractionTarget.DM -> !ctx.event.isFromGuild
        }

        if (!result) throw InteractionTargetMismatch(ctx, annotation.fullName, annotation.target)
    }

    override suspend fun execute(ctx: SlashCommandContext) {
        checkTarget(ctx)
        function.callSuspend(this, ctx, *options.map { it.compile(ctx) }.toTypedArray())
    }

    override fun toString() = SlashUtils.handlerToString(this)

    companion object {
        private val log = LoggerFactory.getLogger("InteractionHandler")

        private fun buildHandlerParameters(handler: SlashCommandHandler, target: InteractionTarget): List<FunctionParameter> {
            require(!handler.function.parameters.any { it.isVararg }) {
                "SlashCommand cannot have varargs parameters: $handler"
            }

            val parametersList = mutableListOf<FunctionParameter>()

            val valueParameters = handler.function.valueParameters

            require(valueParameters.isNotEmpty()) {
                "Not enough parameters: $handler"
            }

            val contextClassifier = valueParameters.first().type.classifier

            require(contextClassifier is SlashCommandContext) {
                "The first parameter of a SlashCommand have to be SlashCommandContext and is $contextClassifier -> $handler"
            }

            when (target) {
                InteractionTarget.ALL, InteractionTarget.DM -> {
                    check(contextClassifier !is GuildSlashCommandContext) {
                        "Do not use GuildSlashCommandContext with a non-guild InteractionTarget, use SlashCommandContext instead -> $handler"
                    }
                }
                InteractionTarget.GUILD -> {
                    if (contextClassifier !is GuildSlashCommandContext) {
                        log.warn("You are not using GuildSlashCommandContext on a guild InteractionTarget -> $handler")
                    }
                }
            }

            if (valueParameters.size <= 1) return parametersList

            val validParams = valueParameters.subList(1, valueParameters.size)

            for (param in validParams) {
                val name = param.findAnnotation<OptionName>()?.value ?: param.name!!
                val kType = param.type
                check(ValidOptionTypes.isValidType(kType.classifier)) {
                    "${kType.classifier} is not a valid type for SlashCommand option: $handler"
                }

                parametersList.add(FunctionParameter(handler, name, kType))
            }

            return parametersList
        }
    }
}