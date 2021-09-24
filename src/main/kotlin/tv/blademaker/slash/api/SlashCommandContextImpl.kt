package tv.blademaker.slash.api

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent

class SlashCommandContextImpl(override val event: SlashCommandEvent) : SlashCommandContext