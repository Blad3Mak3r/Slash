package tv.blademaker.slash.internal

import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.protocol.Message
import io.sentry.protocol.User
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tv.blademaker.slash.api.BaseSlashCommand
import tv.blademaker.slash.api.PermissionTarget
import tv.blademaker.slash.api.SlashCommandClient
import tv.blademaker.slash.api.SlashCommandContext
import tv.blademaker.slash.api.annotations.Permissions
import tv.blademaker.slash.api.exceptions.PermissionsLackException
import java.lang.reflect.Modifier

object SlashUtils {
    private val LOGGER = LoggerFactory.getLogger(SlashUtils::class.java)

    internal fun Array<Permission>.toHuman(jump: Boolean = false): String {
        return this.joinToString(if (jump) "\n" else ", ") { it.getName() }
    }

    internal fun checkPermissions(ctx: SlashCommandContext, permissions: Permissions?) {
        if (permissions == null || permissions.bot.isEmpty() && permissions.user.isEmpty()) return

        var member: Member = ctx.member

        // Check for the user permissions
        var guildPerms = member.hasPermission(permissions.user.toList())
        var channelPerms = member.hasPermission(ctx.channel, permissions.user.toList())

        if (!(guildPerms && channelPerms)) {
            throw PermissionsLackException(ctx, PermissionTarget.USER, permissions.user)
        }

        // Check for the bot permissions
        member = ctx.selfMember
        guildPerms = member.hasPermission(permissions.bot.toList())
        channelPerms = member.hasPermission(ctx.channel, permissions.bot.toList())

        if (!(guildPerms && channelPerms)) {
            throw PermissionsLackException(ctx, PermissionTarget.BOT, permissions.bot)
        }
    }

    internal fun captureSlashCommandException(ctx: SlashCommandContext, e: Throwable, logger: Logger? = null) {
        val message = "Exception executing handler for `${ctx.event.commandPath}` -> **${e.message}**"

        if (ctx.event.isAcknowledged) ctx.sendMessage(message).setEphemeral(true).queue()
        else ctx.reply(message).setEphemeral(true).queue()

        val errorMessage = "Exception executing handler for ${ctx.event.commandPath}, ${e.message}"

        val userType: String = when {
            ctx.author.isSystem -> "System"
            ctx.author.isBot -> "Bot"
            else -> "User"
        }

        Sentry.captureEvent(SentryEvent().apply {
            this.message = Message().apply {
                this.message = errorMessage
            }
            this.user = User().apply {
                this.id = ctx.author.id
                this.username = ctx.author.asTag
                this.others = mapOf("type" to userType)
            }
            this.setExtra("Guild", "${ctx.guild.name} (${ctx.guild.id})")
            this.setExtra("Command Path", ctx.event.commandString)
            throwable = e
        })

        logger?.error(errorMessage, e)
    }

    /**
     * Discover the [BaseSlashCommand] inside the package.
     *
     * @param packageName The package to lookup.
     *
     * @throws IllegalStateException When you try to register more than 1 command with the same name,
     * a command that contains a default handler with sub-commands handlers, a command contains more
     * than 1 handler for the same command path (command/group/subcommand) or a command does not contain
     * handlers.
     *
     * @throws NoSuchMethodException When cannot initialize a command class with no empty constructor.
     *
     * @see java.lang.Class.getDeclaredConstructor
     * @see java.lang.reflect.Constructor.newInstance
     * @see tv.blademaker.slash.api.BaseSlashCommand
     */
    fun discoverSlashCommands(packageName: String): List<BaseSlashCommand> {
        val classes = Reflections(packageName, Scanners.SubTypes)
            .getSubTypesOf(BaseSlashCommand::class.java)
            .filter { !Modifier.isAbstract(it.modifiers) && BaseSlashCommand::class.java.isAssignableFrom(it) }

        LOGGER.info("Discovered a total of ${classes.size} slash commands in package $packageName")

        val commands = mutableListOf<BaseSlashCommand>()

        for (clazz in classes) {
            val instance = clazz.getDeclaredConstructor().newInstance()
            val commandName = instance.commandName.lowercase()

            if (commands.any { it.commandName.equals(commandName, true) }) {
                error("Command with name $commandName is already registered.")
            }

            commands.add(instance)
        }

        return commands
    }

    @Suppress("unused")
    fun RestAction<*>.asEphemeral(): RestAction<*> {
        when(this) {
            is ReplyAction -> this.setEphemeral(true)
            is WebhookMessageAction<*> -> this.setEphemeral(true)
        }

        return this
    }
}