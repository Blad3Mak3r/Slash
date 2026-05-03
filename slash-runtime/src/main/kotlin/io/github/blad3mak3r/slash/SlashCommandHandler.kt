package io.github.blad3mak3r.slash

import io.github.blad3mak3r.slash.context.AutoCompleteContext
import io.github.blad3mak3r.slash.context.ButtonContext
import io.github.blad3mak3r.slash.context.ModalContext
import io.github.blad3mak3r.slash.context.SlashCommandContext
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

/**
 * Contract implemented by every generated `*Command` interface (and in turn by every
 * concrete handler class the user writes).
 *
 * The Gradle plugin generates one interface per command definition, e.g.:
 * ```
 * interface BanCommand : SlashCommandHandler { … }
 * ```
 * The user then provides a concrete class:
 * ```
 * class BanCommandHandler : BanCommand { … }
 * ```
 */
interface SlashCommandHandler {
    fun getCommandName(): String
    fun buildCommandData(): SlashCommandData

    suspend fun dispatch(ctx: SlashCommandContext)

    suspend fun dispatchButton(ctx: ButtonContext): Boolean = false
    suspend fun dispatchModal(ctx: ModalContext): Boolean = false
    suspend fun dispatchAutoComplete(ctx: AutoCompleteContext): Boolean = false
}
