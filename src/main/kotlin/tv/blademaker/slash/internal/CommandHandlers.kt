package tv.blademaker.slash.internal

internal class CommandHandlers(
    val slash: List<SlashCommandHandler>,
    val autoComplete: List<AutoCompleteHandler>
)