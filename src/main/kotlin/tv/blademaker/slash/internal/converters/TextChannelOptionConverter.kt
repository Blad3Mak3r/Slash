package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object TextChannelOptionConverter : OptionConverter<TextChannel> {
    override fun convert(optionMapping: OptionMapping?): TextChannel? {
        return optionMapping?.asGuildChannel?.let { if (it is TextChannel) it else null }
    }
}