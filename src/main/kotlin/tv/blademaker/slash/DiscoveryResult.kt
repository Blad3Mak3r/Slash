package tv.blademaker.slash

import tv.blademaker.slash.internal.AutoCompleteHandler
import tv.blademaker.slash.internal.ButtonHandler
import tv.blademaker.slash.internal.ModalHandler
import tv.blademaker.slash.internal.SlashCommandHandler

data class DiscoveryResult(
    val onAutoComplete: List<AutoCompleteHandler>,
    val onButton: List<ButtonHandler>,
    val onModal: List<ModalHandler>,
    val onSlashCommand: List<SlashCommandHandler>,
)
