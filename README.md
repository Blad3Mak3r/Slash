# Slash
### 🚧 This project is currently in active development 🚧

## Info
Slash is a library that works on JDA for a simple implementation of Slash Commands for Discord.

**This library does not synchronize the commands created with the commands published on Discord.**

## Basic command
Create a command inside the package ``net.example.commands`` called ``PingCommand.kt``:

```kotlin
class PingCommand : BaseSlashCommand("ping") {

    @SlashCommand
    suspend fun default(ctx: SlashCommandContext) {
        ctx.acknowledge(true).queue()

        val restPing = ctx.jda.restPing.await()
        val gatewayPing = ctx.jda.gatewayPing

        ctx.sendEmbed {
            setTitle("Pong!")
            addField("Rest", "${restPing}ms", true)
            addField("Gateway", "${gatewayPing}ms", true)
        }.queue()
    }

}
```

## Sub-commands slash command with permissions
Create a command inside package ``net.example.commands`` called ``RoleCommand.kt``:

```kotlin
class RoleCommand : BaseSlashCommand("role") {
    
    // The parsed path is role/add
    // This handler required MANAGE_ROLES permission fot both, bot and user who execute the command.
    @SlashCommand("add")
    @Permissions(bot = [Permission.MANAGE_ROLES], user = [Permission.MANAGE_ROLES])
    suspend fun addRole(ctx: SlashCommandContext, member: Member) {
        // This handler will add a role to the member.
    }

    // The parsed path is role/remove
    // This handler required MANAGE_ROLES permission fot both, bot and user who execute the command.
    @SlashCommand("remove")
    @Permissions(bot = [Permission.MANAGE_ROLES], user = [Permission.MANAGE_ROLES])
    suspend fun removeRole(ctx: SlashCommandContext, member: Member) {
        // This handler will remove a role to a member if the member have the role.
    }

    // The parsed path is role/list
    @SlashCommand("list")
    suspend fun listRoles(ctx: SlashCommandContext, member: Member?) {
        // This handler have a nullable param, that means the option on the command event
        // can be null.
    }

    // The parsed path is role/compare
    @SlashCommand("compare")
    suspend fun compareRoles(ctx: SlashCommandContext, member1: Member, member2: Member) {
        // This handler will compare the roles between two members from the guild.
    }
}
```

This command will create 4 handlers with the following user representation:
- /role add: (member)
- /role remove: (member)
- /role list: (member?)
- /role compare: (member) (member)

## Advanced commands with sub-command groups and permissions
Create a command inside package ``net.example.commands`` called ``TwitchCommand.kt``:

```kotlin
class TwitchCommand : BaseSlashCommand("twitch") {

    // The parsed path is twitch/clips/top
    @SlashCommand(group = "clips", name = "top")
    @Permissions(bot = [Permission.MESSAGE_EMBED_LINKS])
    suspend fun clipTop(ctx: SlashCommandContext, channel: String?) {
        
    }

    // The parsed path is twitch/clips/random
    @SlashCommand(group = "clips", name = "random")
    @Permissions(bot = [Permission.MESSAGE_EMBED_LINKS])
    suspend fun clipRandom(ctx: SlashCommandContext, channel: String?) {
        
    }
}
```
This command will create 2 handlers with the following user representation:
- /twitch clips top (channel?)
- /twitch clips random (channel?)

## Registering commands
Register ``DefaultCommandClient()`` with the package name where the commands are located, and register
the event listener in your JDA or ShardManager builder.

```kotlin
val commandClient = DefaultCommandClient("com.example.commands").apply {
    
    // Register the event listener
    withShardManager(shardManager)
    // or
    withJDA(jda)
    
    // Expose prometheus statistics to your current metrics collection
    withMetrics()
}
```

``commandClient`` will register ``PingCommand``, ``RoleCommand`` and ``TwitchCommand``.

## Installation
Maven Central comming soon...
```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/Blad3Mak3r/Slash")
        credentials {
            username = "username"
            password = "password"
        }
    }
}
```
