package tv.blademaker.slash.internal

import tv.blademaker.slash.SlashUtils
import tv.blademaker.slash.annotations.OnModal
import tv.blademaker.slash.annotations.matcher
import tv.blademaker.slash.context.ModalContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend

class ModalHandler(
    override val annotation: OnModal,
    override val function: KFunction<*>
) : Handler<OnModal, ModalContext> {

    fun matcher(input: String) = annotation.matcher(input)

    fun matches(input: String) = matcher(input).matches()

    override suspend fun execute(ctx: ModalContext) {
        function.callSuspend(this, ctx)
    }

    override fun toString() = SlashUtils.handlerToString(this)
}