package tv.blademaker.slash.context

import net.dv8tion.jda.api.interactions.commands.OptionMapping

interface InteractionContext {

    fun getOptionOrNull(name: String): OptionMapping?

}