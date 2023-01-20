package tv.blademaker.slash.context

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface DeferrableInteraction {
    /**
     * Automatically detect if the interaction is already acknowledge and if not
     * will acknowledge it.
     *
     *
     */
    suspend fun acknowledge(ephemeral: Boolean = false)

    suspend fun acknowledgeAsync(ephemeral: Boolean): Deferred<Unit> {
        return coroutineScope { async { acknowledge(ephemeral) } }
    }
}