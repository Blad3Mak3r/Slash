package tv.blademaker.slash.internal

import tv.blademaker.slash.api.BaseSlashCommand
import tv.blademaker.slash.api.SlashCommandContext
import tv.blademaker.slash.api.annotations.Option
import tv.blademaker.slash.api.annotations.Permissions
import tv.blademaker.slash.api.annotations.SlashCommand
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

internal class InteractionHandler(
    private val command: BaseSlashCommand,
    private val function: KFunction<*>
) {

    private val annotation: SlashCommand = function.findAnnotation()!!
    val permissions: Permissions? = function.findAnnotation()

    val path = buildString {
        append(command.commandName)
        if (annotation.group.isNotBlank()) append("/${annotation.group}")
        if (annotation.name.isNotBlank()) append("/${annotation.name}")
    }

    private val options: List<FunctionParameter> = buildHandlerParameters(command, function)

    suspend fun execute(instance: BaseSlashCommand, ctx: SlashCommandContext) {
        val params = options.map { it.compile(ctx) }.toTypedArray()
        function.callSuspend(instance, ctx, *params)
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
                val name = param.name!!
                val kType = param.type
                check(ValidOptionTypes.isValidType(kType.classifier)) {
                    "${kType.classifier} is not a valid type for SlashCommand option: ${function.name}"
                }

                parametersList.add(FunctionParameter(command, function, name, kType, param.isOptional))
            }

            return parametersList
        }
    }
}