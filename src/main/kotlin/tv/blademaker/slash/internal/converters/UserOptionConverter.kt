package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object UserOptionConverter : OptionConverter<User> {
    override fun convert(optionMapping: OptionMapping?): User? {
        return optionMapping?.asUser
    }
}