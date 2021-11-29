package tv.blademaker.slash.internal

import tv.blademaker.slash.api.SlashCommandContext

typealias CommandExecutionCheck = suspend (ctx: SlashCommandContext) -> Boolean