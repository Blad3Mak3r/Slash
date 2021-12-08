package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object IntOptionConverter : OptionConverter<Int> {
    override fun convert(optionMapping: OptionMapping?): Int? {
        return optionMapping?.asString?.toInt()
    }
}