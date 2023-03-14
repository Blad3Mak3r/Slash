package tv.blademaker.slash.internal

import tv.blademaker.slash.context.InteractionContext
import kotlin.reflect.KFunction

interface Handler<A : Annotation, C : InteractionContext<*>> {
    val annotation: A
    val function: KFunction<*>

    suspend fun execute(ctx: C)
}