package tv.blademaker.slash.metrics

data class MetricsStrategy(
    val baseName: String = "slash",
    val executedCommands: Boolean = true,
    val successfulCommands: Boolean = true,
    val failedCommands: Boolean = true,
    val measureTime: Boolean = true
)
