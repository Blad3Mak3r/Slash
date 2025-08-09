package io.github.blad3mak3r.slash.internal

import io.github.blad3mak3r.slash.BaseSlashCommand
import io.github.blad3mak3r.slash.annotations.OnAutoComplete
import io.github.blad3mak3r.slash.annotations.OptionName
import io.github.blad3mak3r.slash.context.AutoCompleteContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

class AutoCompleteHandler(
    override val parent: BaseSlashCommand,
    override val function: KFunction<*>
) : Handler {

    private val annotation: OnAutoComplete = function.findAnnotation()!!

    val optionName: String
        get() = annotation.optionName

    override val path = buildString {
        append(parent.commandName)
        if (annotation.group.isNotBlank()) append("/${annotation.group}")
        if (annotation.name.isNotBlank()) append("/${annotation.name}")
    }

    private val options: List<FunctionParameter> = buildHandlerParameters(parent, function)

    suspend fun execute(ctx: AutoCompleteContext) {
        function.callSuspend(parent, ctx, *options.map { it.compile(ctx) }.toTypedArray())
    }

    companion object {
        private fun buildHandlerParameters(command: BaseSlashCommand, function: KFunction<*>): List<FunctionParameter> {
            check(!function.parameters.any { it.isVararg }) {
                "SlashCommand cannot have varargs parameters: ${function.name}"
            }

            val parametersList = mutableListOf<FunctionParameter>()

            check(function.parameters.size in 2..3) {
                "Not valid parameters count: ${command.commandName} -> ${function.name}"
            }

            check(function.parameters[1].type.classifier == AutoCompleteContext::class) {
                "The first parameter of a AutoComplete have to be AutoCompleteContext: ${function.parameters.first().type.classifier}"
            }

            if (function.parameters.size <= 2) return parametersList

            val param = function.parameters[2]
            val name = param.findAnnotation<OptionName>()?.value ?: param.name!!
            val kType = param.type
            check(ValidOptionTypes.isValidType(kType.classifier)) {
                "${kType.classifier} is not a valid type for AutoComplete option: ${function.name}"
            }

            parametersList.add(FunctionParameter(command, function, name, kType))

            return parametersList
        }
    }
}