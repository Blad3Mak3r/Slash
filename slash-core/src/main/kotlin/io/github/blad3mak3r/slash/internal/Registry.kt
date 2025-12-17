package io.github.blad3mak3r.slash.internal

import io.github.blad3mak3r.slash.BaseSlashCommand
import io.github.blad3mak3r.slash.DiscoveryResult
import io.github.blad3mak3r.slash.MessageCommand
import io.github.blad3mak3r.slash.UserCommand

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
