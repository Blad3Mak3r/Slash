package tv.blademaker.slash.internal.converters

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.internal.OptionConverter

internal object MemberOptionConverter : OptionConverter<Member> {
    override fun convert(optionMapping: OptionMapping?): Member? {
        return optionMapping?.asMember
    }
}