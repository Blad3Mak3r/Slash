package io.github.blad3mak3r.slash.registry

import io.github.blad3mak3r.slash.annotations.SlashPrecondition
import io.github.blad3mak3r.slash.context.SlashCommandContext

/**
 * Runtime contract for command preconditions.
 *
 * Implement this interface and annotate your command (or handler function) with
 * `@Require(MyPrecondition::class)` so that the KSP-generated registrar instantiates
 * and evaluates it before executing the handler.
 *
 * @see SlashPrecondition
 */
interface Precondition : SlashPrecondition {

    /**
     * Evaluates whether the interaction is allowed to proceed.
     *
     * @param ctx The slash-command context for the current interaction.
     * @return `true` to allow execution, `false` to abort silently.
     */
    suspend fun check(ctx: SlashCommandContext): Boolean
}
