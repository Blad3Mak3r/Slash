package io.github.blad3mak3r.slash

data class DiscoveryResult(
    val elapsedTime: Long,
    val slashCommands: List<io.github.blad3mak3r.slash.BaseSlashCommand>,
    val userCommands: List<io.github.blad3mak3r.slash.UserCommand>,
    val messageCommands: List<io.github.blad3mak3r.slash.MessageCommand>
)
