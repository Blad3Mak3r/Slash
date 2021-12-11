package tv.blademaker.slash.api

data class DiscoveryResult(
    val elapsedTime: Long,
    val count: Int,
    val commands: List<BaseSlashCommand>
)
