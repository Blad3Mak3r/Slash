package tv.blademaker.slash.internal.handlers

import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.annotations.OnButton
import tv.blademaker.slash.annotations.matcher
import tv.blademaker.slash.context.ButtonContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation

class ButtonHandler(
    override val parent: BaseSlashCommand,
    override val function: KFunction<*>
) : Handler {

    private val annotation: OnButton = function.findAnnotation()!!

    override val path = buildString {
        append(annotation.buttonId)
    }

    fun matcher(input: String) = annotation.matcher(input)

    fun matches(input: String) = matcher(input).matches()

    suspend fun execute(ctx: ButtonContext) {
        function.callSuspend(parent, ctx)
    }
}