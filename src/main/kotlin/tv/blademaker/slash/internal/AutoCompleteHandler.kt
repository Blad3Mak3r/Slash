package tv.blademaker.slash.internal

import tv.blademaker.slash.SlashUtils
import tv.blademaker.slash.annotations.OnAutoComplete
import tv.blademaker.slash.annotations.OptionName
import tv.blademaker.slash.context.AutoCompleteContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters

class AutoCompleteHandler(
    override val annotation: OnAutoComplete,
    override val function: KFunction<*>
) : Handler<OnAutoComplete, AutoCompleteContext> {

    constructor(entry: Map.Entry<OnAutoComplete, KFunction<*>>) : this(
        annotation = entry.key,
        function = entry.value
    )

    val optionName: String
        get() = annotation.optionName

    private val options: List<FunctionParameter> = buildHandlerParameters(this)

    override suspend fun execute(ctx: AutoCompleteContext) {
        function.callSuspend(this, ctx, *options.map { it.compile(ctx) }.toTypedArray())
    }

    override fun toString() = SlashUtils.handlerToString(this)

    companion object {
        private fun buildHandlerParameters(handler: AutoCompleteHandler): List<FunctionParameter> {
            require(!handler.function.parameters.any { it.isVararg }) {
                "AutoComplete cannot have varargs parameters: : $handler"
            }

            val valueParameters = handler.function.valueParameters

            val parametersList = mutableListOf<FunctionParameter>()

            require(valueParameters.size in 1..2) {
                "Not valid parameters count: $handler"
            }

            val contextClassifier = valueParameters.first().type.classifier

            require(contextClassifier == AutoCompleteContext::class.java) {
                "The first parameter of $handler have to be AutoCompleteContext: $contextClassifier"
            }

            if (valueParameters.size <= 1) return parametersList

            val param = valueParameters[1]
            val name = param.findAnnotation<OptionName>()?.value ?: param.name!!
            val kType = param.type
            check(ValidOptionTypes.isValidType(kType.classifier)) {
                "${kType.classifier} is not a valid type for AutoComplete option: $handler"
            }

            parametersList.add(FunctionParameter(handler, name, kType))

            return parametersList
        }
    }
}