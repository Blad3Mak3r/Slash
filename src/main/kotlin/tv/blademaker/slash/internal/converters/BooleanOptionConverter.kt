package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object BooleanOptionConverter : OptionConverter<Boolean> {
    override fun convert(optionMapping: OptionMapping?): Boolean? {
        return optionMapping?.asBoolean
    }
}