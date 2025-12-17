package io.github.blad3mak3r.slash.internal

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import kotlin.reflect.KClassifier
import kotlin.reflect.KType

enum class ValidOptionTypes(
    private val kClass: KClassifier,
    private val converter: OptionConverter<*>
) {
    ATTACHMENT(Message.Attachment::class, OptionConverter.AttachmentOption),
    STRING(String::class, OptionConverter.StringOption),
    LONG(Long::class, OptionConverter.LongOption),
    BOOLEAN(Boolean::class, OptionConverter.BooleanOption),
    MEMBER(Member::class, OptionConverter.MemberOption),
    USER(User::class, OptionConverter.UserOption),
    GUILD_CATEGORY(Category::class, OptionConverter.GuildCategoryOption),
    GUILD_CHANNEL(GuildChannel::class, OptionConverter.StandardGuildChannelOption),
    GUILD_MESSAGE_CHANNEL(GuildMessageChannel::class, OptionConverter.GuildMessageChannelOption),
    ROLE(Role::class, OptionConverter.RoleOption),
    INTEGER(Int::class, OptionConverter.IntOption),
    FLOAT(Float::class, OptionConverter.FloatOption),
    DOUBLE(Double::class, OptionConverter.DoubleOption),
    TEXT_CHANNEL(TextChannel::class, OptionConverter.TextChannelOption),
    AUDIO_CHANNEL(AudioChannel::class, OptionConverter.AudioChannelOption),
    STAGE_CHANNEL(StageChannel::class, OptionConverter.StageChannelOption),
    VOICE_CHANNEL(VoiceChannel::class, OptionConverter.VoiceChannelOption);

    fun convert(option: OptionMapping?) = converter.convert(option)

    companion object {
        fun isValidType(type: KClassifier?): Boolean {
            return if (type == null) false
            else entries.any { it.kClass == type }
        }

        fun get(type: KType) = entries.find { it.kClass == type.classifier }
    }
}