package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object LongOptionConverter : OptionConverter<Long> {
    override fun convert(optionMapping: OptionMapping?): Long? {
        return optionMapping?.asLong
    }
}