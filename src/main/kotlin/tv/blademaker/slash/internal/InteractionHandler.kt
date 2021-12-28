package tv.blademaker.slash.internal

import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.annotations.*
import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.context.AutoCompleteContext
import tv.blademaker.slash.exceptions.InteractionTargetMismatch
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

internal class InteractionHandler(
    private val command: BaseSlashCommand,
    private val function: KFunction<*>
) : Handler {

    private val annotation: SlashCommand = function.findAnnotation()!!
    val permissions: Permissions? = function.findAnnotation()

    override val path = buildString {
        append(command.commandName)
        if (annotation.group.isNotBlank()) append("/${annotation.group}")
        if (annotation.name.isNotBlank()) append("/${annotation.name}")
    }

    private val options: List<FunctionParameter> = buildHandlerParameters(command, function)

    private fun checkTarget(ctx: SlashCommandContext) {
        val result = when (annotation.target) {
            InteractionTarget.ALL -> true
            InteractionTarget.GUILD -> ctx.event.isFromGuild
            InteractionTarget.DM -> !ctx.event.isFromGuild
        }

        if (!result) throw InteractionTargetMismatch(ctx, path, annotation.target)
    }

    suspend fun execute(ctx: SlashCommandContext) {
        checkTarget(ctx)
        val params = options.map { it.compile(ctx) }.toTypedArray()
        function.callSuspend(command, ctx, *params)
    }

    companion object {
        private fun buildHandlerParameters(command: BaseSlashCommand, function: KFunction<*>): List<FunctionParameter> {
            check(!function.parameters.any { it.isVararg }) {
                "SlashCommand cannot have varargs parameters: ${function.name}"
            }

            val parametersList = mutableListOf<FunctionParameter>()

            check(function.parameters.size >= 2) {
                "Not enough parameters: ${command.commandName} -> ${function.name}"
            }

            check(function.parameters[1].type.classifier == SlashCommandContext::class) {
                "The first parameter of a SlashCommand have to be SlashCommandContext: ${function.parameters.first().type.classifier}"
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