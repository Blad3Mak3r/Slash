package examples

import io.github.blad3mak3r.slash.annotations.ApplicationCommand
import io.github.blad3mak3r.slash.annotations.InteractionTarget
import io.github.blad3mak3r.slash.annotations.OnSlashCommand
import io.github.blad3mak3r.slash.annotations.OptionName
import io.github.blad3mak3r.slash.context.GuildSlashCommandContext
import io.github.blad3mak3r.slash.context.SlashCommandContext
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

/**
 * Exhaustive parameter-type coverage command.
 *
 * Each subcommand tests a different set of supported option types,
 * both non-null and nullable variants.
 *
 * /types primitives  — String, Long, Int (@OptionName), Boolean, Double, Float
 * /types nullable    — String?, Long?, Boolean?, Double?
 * /types entities    — Member, User, Role  (guild target)
 * /types nullable-entities — Member?, User?, Role?
 * /types misc        — Message.Attachment, IMentionable
 * /types channels    — GuildChannel, TextChannel, VoiceChannel? (guild target)
 * /types channels-extended — GuildMessageChannel, AudioChannel, Category?, StageChannel? (guild target)
 */
@ApplicationCommand(name = "types")
class AllTypesCommand {

    // ── Primitive / scalar types ──────────────────────────────────────────────

    @OnSlashCommand(name = "primitives")
    suspend fun primitives(
        ctx: SlashCommandContext,
        text: String,
        count: Long,
        @OptionName("amount") amount: Int,
        enabled: Boolean,
        ratio: Double,
        scale: Float
    ) {
        ctx.replyMessage(
            "text=$text count=$count amount=$amount enabled=$enabled ratio=$ratio scale=$scale"
        ).queue()
    }

    @OnSlashCommand(name = "nullable")
    suspend fun nullable(
        ctx: SlashCommandContext,
        text: String?,
        count: Long?,
        enabled: Boolean?,
        ratio: Double?
    ) {
        ctx.replyMessage(
            "text=$text count=$count enabled=$enabled ratio=$ratio"
        ).queue()
    }

    // ── Discord entity types ──────────────────────────────────────────────────

    @OnSlashCommand(name = "entities", target = InteractionTarget.GUILD)
    suspend fun entities(
        ctx: GuildSlashCommandContext,
        member: Member,
        user: User,
        role: Role
    ) {
        ctx.replyMessage(
            "member=${member.effectiveName} user=${user.name} role=${role.name}"
        ).queue()
    }

    @OnSlashCommand(name = "nullable-entities")
    suspend fun nullableEntities(
        ctx: SlashCommandContext,
        member: Member?,
        user: User?,
        role: Role?
    ) {
        ctx.replyMessage(
            "member=${member?.effectiveName} user=${user?.name} role=${role?.name}"
        ).queue()
    }

    // ── Attachment + Mentionable ──────────────────────────────────────────────

    @OnSlashCommand(name = "misc")
    suspend fun misc(
        ctx: SlashCommandContext,
        attachment: Message.Attachment,
        mentionable: IMentionable
    ) {
        ctx.replyMessage(
            "attachment=${attachment.fileName} mentionable=${mentionable.asMention}"
        ).queue()
    }

    // ── Channel types ─────────────────────────────────────────────────────────

    @OnSlashCommand(name = "channels", target = InteractionTarget.GUILD)
    suspend fun channels(
        ctx: GuildSlashCommandContext,
        channel: GuildChannel,      // GuildChannelUnion assignable to GuildChannel — no cast
        textChannel: TextChannel,   // non-null → generates: asChannel as TextChannel
        optVoice: VoiceChannel?     // nullable  → generates: asChannel as? VoiceChannel
    ) {
        ctx.replyMessage(
            "channel=${channel.name} text=${textChannel.name} voice=${optVoice?.name}"
        ).queue()
    }

    @OnSlashCommand(name = "channels-extended", target = InteractionTarget.GUILD)
    suspend fun channelsExtended(
        ctx: GuildSlashCommandContext,
        msgChannel: GuildMessageChannel,  // non-null → generates: asChannel as GuildMessageChannel
        audioChannel: AudioChannel,       // non-null → generates: asChannel as AudioChannel
        category: Category?,              // nullable → generates: asChannel as? Category
        stage: StageChannel?              // nullable → generates: asChannel as? StageChannel
    ) {
        ctx.replyMessage(
            "msg=${msgChannel.name} audio=${audioChannel.name} cat=${category?.name} stage=${stage?.name}"
        ).queue()
    }
}
