package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object MessageChannelOptionConverter : OptionConverter<MessageChannel> {
    override fun convert(optionMapping: OptionMapping?): MessageChannel? {
        return optionMapping?.asMessageChannel
    }
}