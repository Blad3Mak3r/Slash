package tv.blademaker.slash

data class DiscoveryResult(
    val elapsedTime: Long,
    val commands: List<BaseSlashCommand>
)
