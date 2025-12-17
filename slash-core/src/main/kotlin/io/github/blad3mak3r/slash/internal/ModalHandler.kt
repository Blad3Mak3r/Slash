package io.github.blad3mak3r.slash.internal

import io.github.blad3mak3r.slash.BaseSlashCommand
import io.github.blad3mak3r.slash.annotations.OnModal
import io.github.blad3mak3r.slash.annotations.matcher
import io.github.blad3mak3r.slash.context.ModalContext
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