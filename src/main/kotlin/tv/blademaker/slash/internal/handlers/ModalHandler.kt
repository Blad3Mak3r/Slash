package tv.blademaker.slash.internal.handlers

import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.annotations.OnModal
import tv.blademaker.slash.annotations.matcher
import tv.blademaker.slash.context.ModalContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

class ModalHandler(
    override val parent: BaseSlashCommand,
    override val function: KFunction<*>
) : Handler {

    private val annotation: OnModal = function.findAnnotation()!!

    override val path = buildString {
        append(annotation.modalId)
    }

    fun matcher(input: String) = annotation.matcher(input)

    fun matches(input: String) = matcher(input).matches()

    suspend fun execute(ctx: ModalContext) {
        function.callSuspend(parent, ctx)
    }
}