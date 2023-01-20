package tv.blademaker.slash.internal

import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.annotations.OnAutoComplete
import tv.blademaker.slash.annotations.OnModal
import tv.blademaker.slash.annotations.matcher
import tv.blademaker.slash.context.AutoCompleteContext
import tv.blademaker.slash.context.ModalContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

class ModalHandler(
    override val parent: BaseSlashCommand,
    internal val function: KFunction<*>
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