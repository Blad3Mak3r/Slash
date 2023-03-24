package tv.blademaker.slash

import tv.blademaker.slash.context.MessageCommandContext
import tv.blademaker.slash.internal.InterceptorHandler
import tv.blademaker.slash.internal.MessageCommandInterceptor

abstract class MessageCommand(val commandName: String) : InterceptorHandler<MessageCommandContext, MessageCommandInterceptor>() {

    abstract suspend fun handle(ctx: MessageCommandContext)

}