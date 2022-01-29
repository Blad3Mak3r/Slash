package tv.blademaker.slash.internal

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.interactions.commands.OptionMapping

internal sealed interface OptionConverter <T : Any> {

    fun convert(optionMapping: OptionMapping?): T?

    object BooleanOption : OptionConverter<Boolean> {
        override fun convert(optionMapping: OptionMapping?): Boolean? {
            return optionMapping?.asBoolean
        }
    }

    object DoubleOption : OptionConverter<Double> {
        override fun convert(optionMapping: OptionMapping?): Double? {
            return optionMapping?.asDouble
        }
    }

    object FloatOption: OptionConverter<Float> {
        override fun convert(optionMapping: OptionMapping?): Float? {
            return optionMapping?.asDouble?.toFloat()
        }
    }

    object GuildChannelOption : OptionConverter<GuildChannel> {
        override fun convert(optionMapping: OptionMapping?): GuildChannel? {
            return optionMapping?.asGuildChannel
        }
    }

    object IntOption : OptionConverter<Int> {
        override fun convert(optionMapping: OptionMapping?): Int? {
            return optionMapping?.asString?.toInt()
        }
    }

    object LongOption : OptionConverter<Long> {
        override fun convert(optionMapping: OptionMapping?): Long? {
            return optionMapping?.asLong
        }
    }

    object MemberOption : OptionConverter<Member> {
        override fun convert(optionMapping: OptionMapping?): Member? {
            return optionMapping?.asMember
        }
    }

    object MessageChannelOption : OptionConverter<MessageChannel> {
        override fun convert(optionMapping: OptionMapping?): MessageChannel? {
            return optionMapping?.asMessageChannel
        }
    }

    object RoleOption : OptionConverter<Role> {
        override fun convert(optionMapping: OptionMapping?): Role? {
            return optionMapping?.asRole
        }
    }

    object StringOption : OptionConverter<String> {
        override fun convert(optionMapping: OptionMapping?): String? {
            return optionMapping?.asString
        }
    }

    object TextChannelOption : OptionConverter<TextChannel> {
        override fun convert(optionMapping: OptionMapping?): TextChannel? {
            return optionMapping?.asGuildChannel?.let { if (it is TextChannel) it else null }
        }
    }

    object UserOption : OptionConverter<User> {
        override fun convert(optionMapping: OptionMapping?): User? {
            return optionMapping?.asUser
        }
    }

    object VoiceChannelOption : OptionConverter<VoiceChannel> {
        override fun convert(optionMapping: OptionMapping?): VoiceChannel? {
            return optionMapping?.asGuildChannel?.let { if (it is VoiceChannel) it else null }
        }
    }
}