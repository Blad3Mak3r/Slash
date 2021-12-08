package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object DoubleOptionConverter : OptionConverter<Double> {
    override fun convert(optionMapping: OptionMapping?): Double? {
        return optionMapping?.asDouble
    }
}