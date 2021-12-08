package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object VoiceChannelOptionConverter : OptionConverter<VoiceChannel> {
    override fun convert(optionMapping: OptionMapping?): VoiceChannel? {
        return optionMapping?.asGuildChannel?.let { if (it is VoiceChannel) it else null }
    }
}