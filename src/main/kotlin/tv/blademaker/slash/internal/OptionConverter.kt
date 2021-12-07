package tv.blademaker.slash.internal

import net.dv8tion.jda.api.interactions.commands.OptionMapping

interface OptionConverter <T : Any> {

    fun convert(optionMapping: OptionMapping?): T?

}