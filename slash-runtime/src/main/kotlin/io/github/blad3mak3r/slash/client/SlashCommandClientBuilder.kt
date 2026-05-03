package io.github.blad3mak3r.slash.client

import io.github.blad3mak3r.slash.SlashRegistry
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager

class SlashCommandClientBuilder internal constructor(private val registry: SlashRegistry) {

    fun build(): SlashCommandClient = SlashCommandClient(registry)

    fun buildWith(jda: JDA): SlashCommandClient {
        val client = build()
        jda.addEventListener(client)
        return client
    }

    fun buildWith(shardManager: ShardManager): SlashCommandClient {
        val client = build()
        shardManager.addEventListener(client)
        return client
    }
}
