package io.github.blad3mak3r.slash

import tv.blademaker.slash.context.UserCommandContext
import tv.blademaker.slash.internal.InterceptorHandler
import tv.blademaker.slash.internal.UserCommandInterceptor

abstract class UserCommand(val commandName: String) : InterceptorHandler<UserCommandContext, UserCommandInterceptor>() {

    abstract suspend fun handle(ctx: UserCommandContext)

}