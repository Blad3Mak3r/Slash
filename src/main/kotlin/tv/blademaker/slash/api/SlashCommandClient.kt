package tv.blademaker.slash.api

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.EventListener
import tv.blademaker.slash.internal.SlashUtils.toHuman

@Suppress("unused")
interface SlashCommandClient : EventListener {

    val registry: List<BaseSlashCommand>

    override fun onEvent(event: GenericEvent) {
        if (event is SlashCommandEvent) onSlashCommandEvent(event)
    }

    fun onSlashCommandEvent(event: SlashCommandEvent)

    fun getCommand(name: String) = registry.firstOrNull { it.commandName.equals(name, true) }

    fun registerMetrics() {
        Metrics.register()
    }

    fun onLackOfPermissions(ctx: SlashCommandContext, target: PermissionTarget, permissions: Array<Permission>) {
        when(target) {
            PermissionTarget.BOT -> {
                val perms = permissions.toHuman()
                ctx.reply("\uD83D\uDEAB The bot does not have the necessary permissions to carry out this action." +
                        "\nRequired permissions: **${perms}**.")
            }
            PermissionTarget.USER -> {
                val perms = permissions.toHuman()
                ctx.reply("\uD83D\uDEAB You do not have the necessary permissions to carry out this action." +
                        "\nRequired permissions: **${perms}**.")
            }
        }.setEphemeral(true).queue()
    }
}