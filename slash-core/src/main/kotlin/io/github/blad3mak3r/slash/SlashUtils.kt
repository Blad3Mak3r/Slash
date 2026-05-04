package io.github.blad3mak3r.slash

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import io.github.blad3mak3r.slash.client.SlashCommandClient
import kotlin.time.Duration

object SlashUtils {

    inline fun <reified E : GenericEvent> on(
        events: SharedFlow<GenericEvent>,
        scope: CoroutineScope,
        noinline action: suspend (event: E) -> Unit
    ): Job {
        SlashCommandClient.log.debug("Registering event collector for ${E::class.java.simpleName}.")
        return events.buffer(Channel.UNLIMITED)
            .filterIsInstance<E>()
            .onEach {
                try {
                    action(it)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    SlashCommandClient.log.error(
                        "Exception handling ${it::class.java.simpleName} [${it.jda.shardInfo}]: ${e.message}", e
                    )
                }
            }
            .launchIn(scope)
    }

    suspend inline fun <reified E : GenericEvent> await(
        events: SharedFlow<GenericEvent>,
        timeout: Duration,
        crossinline filter: suspend (event: E) -> Boolean
    ): E? = withTimeoutOrNull(timeout) {
        SlashCommandClient.log.debug("Registering event waiter for ${E::class.java.simpleName}.")
        events.buffer(Channel.UNLIMITED)
            .cancellable()
            .filterIsInstance<E>()
            .filter(filter)
            .firstOrNull()
    }

    /**
     * Converts an [Array] of [Permission] to a human-readable string.
     *
     * @param jump If `true` the separator is `\n`, otherwise `, `.
     */
    fun Array<Permission>.toHuman(jump: Boolean = false): String {
        return this.joinToString(if (jump) "\n" else ", ") { it.getName() }
    }
}
