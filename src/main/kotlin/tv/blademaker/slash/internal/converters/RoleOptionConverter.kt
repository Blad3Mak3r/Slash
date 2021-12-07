package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object RoleOptionConverter : OptionConverter<Role> {
    override fun convert(optionMapping: OptionMapping?): Role? {
        return optionMapping?.asRole
    }
}