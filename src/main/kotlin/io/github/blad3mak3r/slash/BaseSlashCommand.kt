package io.github.blad3mak3r.slash

import io.github.blad3mak3r.slash.context.SlashCommandContext
import io.github.blad3mak3r.slash.internal.InterceptorHandler
import io.github.blad3mak3r.slash.internal.SlashCommandInterceptor

abstract class BaseSlashCommand(val commandName: String) : InterceptorHandler<SlashCommandContext, SlashCommandInterceptor>()
