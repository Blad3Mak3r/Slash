package io.github.blad3mak3r.slash.extensions

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent

val GenericCommandInteractionEvent.commandPath: String
    get() = buildString {
        append(name)
        if (subcommandGroup != null) {
            append("/")
            append(subcommandGroup)
        }
        if (subcommandName != null) {
            append("/")
            append(subcommandName)
        }
    }

val CommandAutoCompleteInteractionEvent.commandPath: String
    get() = buildString {
        append(name)
        if (subcommandGroup != null) {
            append("/")
            append(subcommandGroup)
        }
        if (subcommandName != null) {
            append("/")
            append(subcommandName)
        }
    }