package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object StringOptionConverter : OptionConverter<String> {
    override fun convert(optionMapping: OptionMapping?): String? {
        return optionMapping?.asString
    }
}