package io.github.blad3mak3r.slash

import io.github.blad3mak3r.slash.context.AutoCompleteContext
import io.github.blad3mak3r.slash.context.ButtonContext
import io.github.blad3mak3r.slash.context.ModalContext
import io.github.blad3mak3r.slash.context.SlashCommandContext
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

/**
 * Base class for every generated `Abstract*CommandHandler`.
 *
 * The Gradle plugin generates a concrete abstract subclass per command definition, adding:
 *  - `buildCommandData()` — builds the JDA [SlashCommandData] from the DSL definition
 *  - `dispatch()` — routes to the correct abstract `on*` method
 *  - `dispatchButton()` / `dispatchModal()` / `dispatchAutoComplete()` — regex-based routing
 */
abstract class AbstractCommandHandler {
    abstract fun getCommandName(): String
    abstract fun buildCommandData(): SlashCommandData

    abstract suspend fun dispatch(ctx: SlashCommandContext)

    open suspend fun dispatchButton(ctx: ButtonContext): Boolean = false
    open suspend fun dispatchModal(ctx: ModalContext): Boolean = false
    open suspend fun dispatchAutoComplete(ctx: AutoCompleteContext): Boolean = false
}
