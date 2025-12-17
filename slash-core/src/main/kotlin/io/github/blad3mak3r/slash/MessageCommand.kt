package io.github.blad3mak3r.slash

import io.github.blad3mak3r.slash.context.MessageCommandContext
import io.github.blad3mak3r.slash.internal.InterceptorHandler
import io.github.blad3mak3r.slash.internal.MessageCommandInterceptor

abstract class MessageCommand(val commandName: String) : io.github.blad3mak3r.slash.internal.InterceptorHandler<io.github.blad3mak3r.slash.context.MessageCommandContext, io.github.blad3mak3r.slash.internal.MessageCommandInterceptor>() {

    abstract suspend fun handle(ctx: io.github.blad3mak3r.slash.context.MessageCommandContext)

}