package tv.blademaker.slash.internal

import net.dv8tion.jda.api.entities.*
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
    GUILD_CHANNEL(GuildChannel::class, OptionConverter.GuildChannelOption),
    MESSAGE_CHANNEL(String::class, OptionConverter.MessageChannelOption),
    ROLE(Role::class, OptionConverter.RoleOption),
    INTEGER(Int::class, OptionConverter.IntOption),
    FLOAT(Float::class, OptionConverter.FloatOption),
    DOUBLE(Double::class, OptionConverter.DoubleOption),
    TEXT_CHANNEL(TextChannel::class, OptionConverter.TextChannelOption),
    VOICE_CHANNEL(VoiceChannel::class, OptionConverter.VoiceChannelOption);

    fun convert(option: OptionMapping?) = converter.convert(option)

    companion object {
        fun isValidType(type: KClassifier?): Boolean {
            return if (type == null) false
            else values().any { it.kClass == type }
        }

        fun get(type: KType) = values().find { it.kClass == type.classifier }
    }
}