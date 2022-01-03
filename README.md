[maven-central-shield]: https://img.shields.io/maven-central/v/tv.blademaker/slash?color=blue
[maven-central]: https://search.maven.org/artifact/tv.blademaker/slash

# Slash [![Maven Central][maven-central-shield]][maven-central]
### 🚧 This project is currently in active development 🚧
Slash is a library that works with JDA for an advanced implementation of Slash Commands for Discord.


## Table of contents
- [ToDo](#todo)
- [Commands creation](#create-commands)
    - [Basic command](#basic-command)
    - [Sub-commands and permissions](#sub-commands-slash-command-with-permissions)
    - [Advanced commands](#advanced-commands-with-sub-command-groups-and-permissions)
    - [Registering commands](#registering-commands)
    - [Using Context Actions](#using-context-actions)
    - [Custom Option names](#custom-option-names)
- [Download](#download)

**This library does not synchronize the commands created with the commands published on Discord.**

## ToDo
- [x] Implement handler for default command.
- [x] Implement handlers for sub-commands.
- [x] Implement handlers for sub-commands groups.
- [ ] Synchronize discord published commands with create commands.
- [ ] Useful docs.
- [ ] Be a nice package :).

## Create commands

### Basic command
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

### Sub-commands slash command with permissions
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

### Advanced commands with sub-command groups and permissions
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

### Registering commands
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

### Using context actions
You can build context actions inside SlashCommands so easy.
```kotlin
@SlashCommand
suspend fun contextActions(ctx: SlashCommandContext) {
    
    // This is a context action
    val embedAction: EmbedContextAction = ctx.embed {
        setTitle("Embed Title")
    }
    0
    val messageAction: MessageContextAction = ctx.message {
        append("Message content")
    }
    
    // To execute an action use send() or reply()
    val messageResult: ReplyAction = messageAction.reply().await()
    
    val embedResult: WebhookMessageAction<Message> = embedAction.send().await()
    
    // You can get the generated message use 'original'.
    val embed = embedAction.original
    
    val message = messageAction.original
    
}
```

### Custom Option names
You can use the annotation [@OptionName](src/main/kotlin/tv/blademaker/slash/api/annotations/OptionName.kt)
the set a custom name for an option.
```kotlin
@SlashCommand
suspend fun customName(ctx: SlashCommandContext, @OptionName("query") option1: String) {
    // the variable option1 will get the content of ctx.getOption("query")!
}
```

## Download
[![Maven Central][maven-central-shield]][maven-central]

### Gradle
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("tv.blademaker:slash:VERSION")
}
```

### Maven
```xml
<dependency>
    <groupId>tv.blademaker</groupId>
    <artifactId>slash</artifactId>
    <version>VERSION</version>
</dependency>
        
```
