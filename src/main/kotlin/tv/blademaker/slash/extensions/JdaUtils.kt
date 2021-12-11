package tv.blademaker.slash.extensions

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import tv.blademaker.slash.client.SlashCommandClient

fun JDABuilder.setSlashCommandClient(slashCommandClient: SlashCommandClient) {
    addEventListeners(slashCommandClient)
}

fun JDA.setSlashCommandClient(slashCommandClient: SlashCommandClient) {
    addEventListener(slashCommandClient)
}

fun DefaultShardManagerBuilder.setSlashCommandClient(slashCommandClient: SlashCommandClient) {
    addEventListeners(slashCommandClient)
}

fun ShardManager.setSlashCommandClient(slashCommandClient: SlashCommandClient) {
    addEventListener(slashCommandClient)
}