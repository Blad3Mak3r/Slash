package tv.blademaker.slash

import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.internal.ExecutionInterceptor

abstract class BaseSlashCommand(val commandName: String) {

    private val interceptors: MutableList<ExecutionInterceptor> = mutableListOf()

    internal suspend fun runInterceptors(ctx: SlashCommandContext): Boolean {
        if (interceptors.isEmpty()) return true
        return interceptors.all { it(ctx) }
    }


    fun addInterceptor(check: ExecutionInterceptor) {
        if (interceptors.contains(check)) error("Check already registered.")
        interceptors.add(check)
    }
}
