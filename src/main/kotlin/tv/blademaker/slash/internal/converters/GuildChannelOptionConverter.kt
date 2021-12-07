package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object GuildChannelOptionConverter : OptionConverter<GuildChannel> {
    override fun convert(optionMapping: OptionMapping?): GuildChannel? {
        return optionMapping?.asGuildChannel
    }
}