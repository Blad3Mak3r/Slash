package io.github.blad3mak3r.slash

/**
 * Implemented by the generated `object SlashCommandRegistry`.
 * Provides the list of [AbstractCommandHandler] instances for the [client][io.github.blad3mak3r.slash.client.SlashCommandClient].
 */
interface SlashRegistry {
    val handlers: List<AbstractCommandHandler>
}
