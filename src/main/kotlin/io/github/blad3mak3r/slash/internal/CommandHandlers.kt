package io.github.blad3mak3r.slash.internal

internal class CommandHandlers(
    val slash: List<SlashCommandHandler>,
    val autoComplete: List<AutoCompleteHandler>,
    val modalHandlers: List<ModalHandler>,
    val buttonHandlers: List<ButtonHandler>
)