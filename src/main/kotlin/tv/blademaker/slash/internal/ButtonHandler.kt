package tv.blademaker.slash.internal

import tv.blademaker.slash.SlashUtils
import tv.blademaker.slash.annotations.OnButton
import tv.blademaker.slash.annotations.matcher
import tv.blademaker.slash.context.ButtonContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend

class ButtonHandler(
    override val annotation: OnButton,
    override val function: KFunction<*>
) : Handler<OnButton, ButtonContext> {

    fun matcher(input: String) = annotation.matcher(input)

    fun matches(input: String) = matcher(input).matches()

    override suspend fun execute(ctx: ButtonContext) {
        function.callSuspend(this, ctx)
    }

    override fun toString() = SlashUtils.handlerToString(this)
}