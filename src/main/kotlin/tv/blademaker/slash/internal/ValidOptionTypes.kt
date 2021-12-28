package tv.blademaker.slash.internal

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.converters.*
import tv.blademaker.slash.internal.converters.BooleanOptionConverter
import tv.blademaker.slash.internal.converters.LongOptionConverter
import tv.blademaker.slash.internal.converters.StringOptionConverter
import tv.blademaker.slash.internal.converters.UserOptionConverter
import kotlin.reflect.KClassifier
import kotlin.reflect.KType

enum class ValidOptionTypes(
    private val kClass: KClassifier,
    private val converter: OptionConverter<*>
) {
    STRING(String::class, StringOptionConverter),
    LONG(Long::class, LongOptionConverter),
    BOOLEAN(Boolean::class, BooleanOptionConverter),
    MEMBER(Member::class, MemberOptionConverter),
    USER(User::class, UserOptionConverter),
    GUILD_CHANNEL(GuildChannel::class, GuildChannelOptionConverter),
    MESSAGE_CHANNEL(String::class, MessageChannelOptionConverter),
    ROLE(Role::class, RoleOptionConverter),
    INTEGER(Int::class, IntOptionConverter),
    FLOAT(Float::class, FloatOptionConverter),
    DOUBLE(Double::class, DoubleOptionConverter),
    TEXT_CHANNEL(TextChannel::class, TextChannelOptionConverter),
    VOICE_CHANNEL(VoiceChannel::class, VoiceChannelOptionConverter);

    fun convert(option: OptionMapping?) = converter.convert(option)

    companion object {
        fun isValidType(type: KClassifier?): Boolean {
            return if (type == null) false
            else values().any { it.kClass == type }
        }

        fun get(type: KType) = values().find { it.kClass == type.classifier }
    }
}