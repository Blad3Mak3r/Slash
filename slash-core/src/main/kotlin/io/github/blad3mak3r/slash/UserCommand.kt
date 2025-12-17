package io.github.blad3mak3r.slash

import io.github.blad3mak3r.slash.context.UserCommandContext
import io.github.blad3mak3r.slash.internal.InterceptorHandler
import io.github.blad3mak3r.slash.internal.UserCommandInterceptor

abstract class UserCommand(val commandName: String) : InterceptorHandler<UserCommandContext, UserCommandInterceptor>() {

    abstract suspend fun handle(ctx: UserCommandContext)

}