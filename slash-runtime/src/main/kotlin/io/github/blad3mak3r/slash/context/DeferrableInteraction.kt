package io.github.blad3mak3r.slash.context

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface DeferrableInteraction {
    suspend fun acknowledge(ephemeral: Boolean = false)

    suspend fun acknowledgeAsync(ephemeral: Boolean = false): Deferred<Unit> = coroutineScope {
        async { acknowledge(ephemeral) }
    }
}
