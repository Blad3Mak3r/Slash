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
import java.lang.reflect.Modifier

object SlashUtils {
    private val LOGGER = LoggerFactory.getLogger(SlashUtils::class.java)

    fun Array<Permission>.toHuman(jump: Boolean = false): String {
        return this.joinToString(if (jump) "\n" else ", ") { it.getName() }
    }

    internal fun hasPermissions(commandClient: SlashCommandClient, ctx: SlashCommandContext, permissions: Permissions?): Boolean {
        if (permissions == null || permissions.bot.isEmpty() && permissions.user.isEmpty()) return true

        var member: Member = ctx.member

        // Check for the user permissions
        var guildPerms = member.hasPermission(permissions.user.toList())
        var channelPerms = member.hasPermission(ctx.channel, permissions.user.toList())

        if (!(guildPerms && channelPerms)) {
            commandClient.onLackOfPermissions(ctx, PermissionTarget.USER, permissions.user)
            return false
        }

        // Check for the bot permissions
        member = ctx.selfMember
        guildPerms = member.hasPermission(permissions.bot.toList())
        channelPerms = member.hasPermission(ctx.channel, permissions.bot.toList())

        if (!(guildPerms && channelPerms)) {
            commandClient.onLackOfPermissions(ctx, PermissionTarget.BOT, permissions.bot)
            return false
        }

        return true
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

    fun discoverSlashCommands(client: SlashCommandClient, packageName: String): List<BaseSlashCommand> {
        val classes = Reflections(packageName, Scanners.SubTypes)
            .getSubTypesOf(BaseSlashCommand::class.java)
            .filter { !Modifier.isAbstract(it.modifiers) && BaseSlashCommand::class.java.isAssignableFrom(it) }

        LOGGER.info("Discovered a total of ${classes.size} slash commands in package $packageName")

        val commands = mutableListOf<BaseSlashCommand>()

        for (clazz in classes) {
            val instance = clazz.getDeclaredConstructor(SlashCommandClient::class.java).newInstance(client)
            val commandName = instance.commandName.lowercase()

            if (commands.any { it.commandName.equals(commandName, true) }) {
                throw IllegalStateException("Command with name $commandName is already registered.")
            }

            commands.add(instance)
        }

        return commands
    }

    private fun optionToString(option: OptionMapping): String {
        return try {
            when (option.type) {
                in LONG_TYPES -> option.asLong.toString()
                OptionType.BOOLEAN -> option.asBoolean.toString()
                else -> option.asString
            }
        } catch (e: Exception) {
            "EXCEPTION"
        }
    }

    private val LONG_TYPES = setOf(OptionType.CHANNEL, OptionType.ROLE, OptionType.USER, OptionType.INTEGER)

    @Suppress("unused")
    fun RestAction<*>.asEphemeral(): RestAction<*> {
        when(this) {
            is ReplyAction -> this.setEphemeral(true)
            is WebhookMessageAction<*> -> this.setEphemeral(true)
        }

        return this
    }
}