package tv.blademaker.slash

import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.internal.InterceptorHandler
import tv.blademaker.slash.internal.SlashCommandInterceptor

abstract class BaseSlashCommand(val commandName: String) : InterceptorHandler<SlashCommandContext, SlashCommandInterceptor>()
