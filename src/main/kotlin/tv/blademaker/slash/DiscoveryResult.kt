package tv.blademaker.slash

data class DiscoveryResult(
    val elapsedTime: Long,
    val slashCommands: List<BaseSlashCommand>,
    val userCommands: List<UserCommand>,
    val messageCommands: List<MessageCommand>
)
