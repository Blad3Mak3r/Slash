[maven-central-shield]: https://img.shields.io/maven-central/v/tv.blademaker/slash?color=blue
[maven-central]: https://search.maven.org/artifact/tv.blademaker/slash
[kotlin]: https://kotlinlang.org/
[jda]: https://github.com/DV8FromTheWorld/JDA/releases/tag/v5.6.1
[jda-rework-interactions]: https://github.com/DV8FromTheWorld/JDA/tree/rework/interactions
[slash-commands]: https://discord.com/developers/docs/interactions/application-commands

# Slash [![Maven Central][maven-central-shield]][maven-central]
### 🚧 This project is currently in active development 🚧
Slash is a library written 100% with **[Kotlin][kotlin]** that works with **[JDA 5.6.1 (Java Discord API)][jda]** for an advanced implementation of **[Application Commands][slash-commands]** for Discord.


## Table of contents
- [ToDo](#todo)
- [Requirements](#requirements)
- [Commands creation](#create-commands)
    - [Basic command](#basic-command)
    - [Sub-commands and permissions](#sub-commands-slash-command-with-permissions)
    - [Advanced commands](#advanced-commands-with-sub-command-groups-and-permissions)
    - [Registering commands](#registering-commands)
    - [Using Context Actions](#using-context-actions)
    - [Custom Option names](#custom-option-names)
    - [Auto complete commands options](#slash-commands-option-auto-complete)
    - [Modals](#modals)
    - [Rate Limiting Command Execution](#rate-limiting)
- [Download](#download)

**This library does not synchronize the commands created with the commands published on Discord.**

## ToDo
- [x] Implement handler for default command.
- [x] Implement handlers for sub-commands.
- [x] Implement handlers for sub-commands groups.
- [x] Add support for **Auto Complete** command interactions.
- [x] Add support for **Modals** command interactions.
- [x] Add support for non-guild commands (DM commands).
- [ ] Synchronize discord published commands with create commands.
- [ ] Useful docs.
- [ ] Be a nice package :).
- [ ] Generate commands at build time.

## Requirements

| Package Name       | Required Version     |
|--------------------|----------------------|
| Kotlinx Coroutines | ``1.7.1``            |
| Java JDK           | ``11``               |
| JDA                | ``5.0.0-beta.19``    |
| Kotlin             | ``1.9.10``           |
| Sentry             | ``7.1.0`` (optional) |

## Create commands

### Basic command
Create a command inside the package ``net.example.commands`` called ``PingCommand.kt``:

```kotlin
class PingCommand : BaseSlashCommand("ping") {

    // This command can be used on guilds and direct messages.
    // SlashCommandContext is used on DM and ALL targets.
    @OnSlashCommand(target = InteractionTarget.ALL)
    suspend fun default(ctx: SlashCommandContext) {
        ctx.acknowledge(true)

        val restPing = ctx.jda.restPing.await()
        val gatewayPing = ctx.jda.gatewayPing

        ctx.embed {
            setTitle("Pong!")
            addField("Rest", "${restPing}ms", true)
            addField("Gateway", "${gatewayPing}ms", true)
        }.queue()
    }

}

class Whois : BaseSlashCommand("whois") {
    
    // This command only can be used on guilds.
    // If you try to use it with SlashCommandContext instead of GuildSlashCommandContext
    // the library will report warms about this.
    @OnSlashCommand(target = InteractionTarget.GUILD)
    suspend fun default(ctx: GuildSlashCommandContext, member: Member) {
        ctx.embed {
            setAuthor(/* ... */)
            setTitle("Whois ${member.asTag}")
            setDescription(/* ... */)
        }.queue()
        // When using queue on a ContextAction will automatically select between
        // reply() and send()
    }
}
```

### Sub-commands slash command with permissions
Create a command inside package ``net.example.commands`` called ``RoleCommand.kt``:

```kotlin
class RoleCommand : BaseSlashCommand("role") {
    
    // The parsed path is role/add
    // This handler required MANAGE_ROLES permission fot both, bot and user who execute the command.
    @OnSlashCommand("add", target = InteractionTarget.GUILD)
    @Permissions(bot = [Permission.MANAGE_ROLES], user = [Permission.MANAGE_ROLES])
    suspend fun addRole(ctx: GuildSlashCommandContext, member: Member) {
        // This handler will add a role to the member.
    }

    // The parsed path is role/remove
    // This handler required MANAGE_ROLES permission fot both, bot and user who execute the command.
    @OnSlashCommand("remove", target = InteractionTarget.GUILD)
    @Permissions(bot = [Permission.MANAGE_ROLES], user = [Permission.MANAGE_ROLES])
    suspend fun removeRole(ctx: GuildSlashCommandContext, member: Member) {
        // This handler will remove a role to a member if the member have the role.
    }

    // The parsed path is role/list
    @OnSlashCommand("list", target = InteractionTarget.GUILD)
    suspend fun listRoles(ctx: GuildSlashCommandContext, member: Member?) {
        // This handler has a nullable param, that means the option on the command event
        // can be null.
    }

    // The parsed path is role/compare
    @OnSlashCommand("compare", target = InteractionTarget.GUILD)
    suspend fun compareRoles(ctx: GuildSlashCommandContext, member1: Member, member2: Member) {
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
    @OnSlashCommand(group = "clips", name = "top", target = InteractionTarget.ALL)
    @Permissions(bot = [Permission.MESSAGE_EMBED_LINKS])
    suspend fun clipTop(ctx: SlashCommandContext, channel: String?) {
        
    }

    // The parsed path is twitch/clips/random
    @OnSlashCommand(group = "clips", name = "random", target = InteractionTarget.ALL)
    @Permissions(bot = [Permission.MESSAGE_EMBED_LINKS])
    suspend fun clipRandom(ctx: SlashCommandContext, channel: String?) {
        
    }
}
```
This command will create 2 handlers with the following user representation:
- /twitch clips top (channel?)
- /twitch clips random (channel?)

### Registering commands
Register the handler using ``SlashCommandClient.default(packageName)`` with the package name where the commands are located, and register
the event listener in your JDA or ShardManager builder.

```kotlin
val shardManager = DefaultShardManagerBuilder().apply { /* ... */ }.build(false)

val commandClient = SlashCommandClient.default("com.example.commands")
  .contextCreator(object : ContextCreator {
    // You can override the default ContextCreator
    
    override suspend fun createContext(event: SlashCommandInteractionEvent): SlashCommandContext {
      return SlashCommandContext.impl(event)
    }

    // SlashCommandContext and GuildSlashCommandContext contains an extra object
    // that is a AtomicReference<Any?> so you can set any object here on the context creation
    // and retrieve it when you handle the command.
    override suspend fun createGuildContext(event: SlashCommandInteractionEvent): GuildSlashCommandContext {
      val context = SlashCommandContext.guild(event)
      context.extra.set(Utils.getGuildConfig(event))
      return context
    }

  })
  .addCheck { ctx ->
    // Imagine you have an ignored channels filter, you can add the global check here.
    if (!ctx.isFromGuild || ctx.guild == null) return true
    
    val cannotExecute = Utils.checkIgnoredChannels(ctx.guild)
    
    return !cannotExecute
  }
  .buildWith(shardManager)
```

``commandClient`` will register ``PingCommand``, ``RoleCommand`` and ``TwitchCommand``.

### Using context actions
You can build context actions inside SlashCommands so easy.
```kotlin
@OnSlashCommand(target = InteractionTarget.ALL)
suspend fun contextActions(ctx: SlashCommandContext) {
    
    // This is a context action
    val embedAction: EmbedContextAction = ctx.embed {
        setTitle("Embed Title")
    }
    
    val messageAction: MessageContextAction = ctx.message {
        append("Message content")
    }
    
    // To execute an action use send() or reply()
    // Only use this if you know if the interaction was acknowledged or
    // you need the response from discord.
    val messageResult: ReplyAction = messageAction.reply().await()
    
    val embedResult: WebhookMessageAction<Message> = embedAction.send().await()
    
    // You can queue the request
    // This will check if the interaction was acknowledged previusly and use the correct behaviour
    ctx.embed {
        setDescription("This is the third message but i dont need to use reply() or send()")
    }.queue()
    
    // When using queue() you can set if the message hast the ephemeral flag or not (by default is set to false)
    ctx.message("This message will be ephemeral").queue(true)
    // Ephemeral messages only are ephemeral when the first reply is ephemeral.
    
    // You can get the generated message use 'original'.
    val embed = embedAction.original
    
    val message = messageAction.original
    
}
```

### Acknowledge Interactions
You have 3 seconds to respond or acknowledge an interaction, you can handle this so easy with the following code.
```kotlin

@OnSlashCommand(target = InteractionTarget.ALL)
suspend fun someCommand(ctx: SlashCommandContext) {
    // If your need to wait before continue the code execution, you can use
    ctx.tryAcknowledge().await()
    // But if the interaction is already acknowledged this will throw an IllegalStateException.
    
    // If you don't know if your interaction was acknowledged and don't need to wait use
    ctx.acknowledge()
  
    // If you want to the interaction follow-up messages to be ephemeral, you need to set true when using the function.
    ctx.acknowledge(true)
}

```

### Custom Option names
You can use the annotation [@OptionName](src/main/kotlin/tv/blademaker/slash/annotations/OptionName.kt)
the set a custom name for an option.
```kotlin
@OnSlashCommand(target = InteractionTarget.ALL)
suspend fun customName(ctx: SlashCommandContext, @OptionName("query") option1: String) {
    // the variable option1 will get the content of ctx.getOption("query")!
}
```

### Rate Limiting Command Execution
Since version **0.6.3** you can rate limit the execution of slash commands based on 3 different targets:
- User
- Channel
- Guild

Configure the rate limited (is not necessary):
```kotlin

val commandHandler = SlashCommandClient.default("com.example.commands")
    .configureRateLimit {
        purgeUnit = TimeUnit.MINUTES
        purgeDelay = 5
        onRateLimitHit = { ctx, rateLimit ->
            // Override default with your implementation.
        }
    }
```

### Slash Commands option auto complete
**AutoCompleteContext** extends **CommandAutoCompleteInteraction**.
```kotlin
@OnAutoComplete("commands", "search", optionName = "query")
suspend fun commandSearchAutocomplete(ctx: AutoCompleteContext) {
    
}
```

### Modals
**ModalContext** extends **ModalInteraction**

**@OnModal** supports RegEx.
```kotlin
@OnModal("feature-request:(.+?)")
suspend fun testModal(ctx: ModalContext) {
    
}
```

### Rate Limiting
```kotlin
@RateLimit(quota = 5, duration = 20, unit = TimeUnit.SECONDS, target = RateLimit.Target.GUILD)
@OnSlashCommand(target = InteractionTarget.GUILD)
fun rateLimitedCommand(ctx: GuildSlashCommand) {
    ctx.message {
        append("This is an example of a rate limited Slash Command, ")
        append("If you hit the limit you will be notified.")
    }.queue()
}
```

[@RateLimit annotation documentation](https://blad3mak3r.github.io/Slash/-slash/tv.blademaker.slash.ratelimit/-rate-limit/index.html)

## Download
[![Maven Central][maven-central-shield]][maven-central]

### Gradle
```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("tv.blademaker:slash:x.y.z")
}
```

### Maven
```xml
<dependency>
    <groupId>tv.blademaker</groupId>
    <artifactId>slash</artifactId>
    <version>x.y.z</version>
</dependency>
        
```