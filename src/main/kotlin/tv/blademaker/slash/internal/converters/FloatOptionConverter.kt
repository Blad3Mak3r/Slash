package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object FloatOptionConverter : OptionConverter<Float> {
    override fun convert(optionMapping: OptionMapping?): Float? {
        return optionMapping?.asDouble?.toFloat()
    }
}