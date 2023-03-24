package tv.blademaker.slash.client

import net.dv8tion.jda.api.hooks.EventListener
import tv.blademaker.slash.exceptions.ExceptionHandler
import tv.blademaker.slash.internal.Registry

@Suppress("unused")
interface SlashCommandClient : EventListener {

    /**
     * The command registry.
     */
    val registry: Registry

    val exceptionHandler: ExceptionHandler

    companion object {
        fun default(packageName: String) = DefaultSlashCommandBuilder(packageName)
    }
}