package io.github.blad3mak3r.slash.internal

import io.github.blad3mak3r.slash.BaseSlashCommand
import io.github.blad3mak3r.slash.annotations.OnButton
import io.github.blad3mak3r.slash.annotations.matcher
import io.github.blad3mak3r.slash.context.ButtonContext
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