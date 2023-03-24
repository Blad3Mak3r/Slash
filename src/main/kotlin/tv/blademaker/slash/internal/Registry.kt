package tv.blademaker.slash.internal

import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.DiscoveryResult
import tv.blademaker.slash.MessageCommand
import tv.blademaker.slash.UserCommand

data class Registry(
    val message: List<MessageCommand>,
    val slash: List<BaseSlashCommand>,
    val user: List<UserCommand>
) {
    companion object {
        fun fromDiscovery(discovery: DiscoveryResult): Registry {
            return Registry(
                message = discovery.messageCommands,
                slash = discovery.slashCommands,
                user = discovery.userCommands
            )
        }
    }
}
