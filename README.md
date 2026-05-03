[maven-central-shield]: https://img.shields.io/maven-central/v/io.github.blad3mak3r/slash-core?color=blue
[maven-central]: https://search.maven.org/artifact/io.github.blad3mak3r/slash
[kotlin]: https://kotlinlang.org/
[jda]: https://github.com/DV8FromTheWorld/JDA
[slash-commands]: https://discord.com/developers/docs/interactions/application-commands

# Slash [![Maven Central][maven-central-shield]][maven-central] [![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/Blad3Mak3r/Slash)
### v2 — zero-reflection, compile-time code generation via KSP

Slash is a library written 100% in **[Kotlin][kotlin]** that works with **[JDA][jda]** for an advanced implementation of **[Application Commands][slash-commands]** for Discord.

All command routing is generated at **compile time** by the included KSP processor. There is no classpath scanning, no `kotlin-reflect`, and no startup overhead from reflection.

---

## Table of contents
- [Requirements](#requirements)
- [Setup (Gradle)](#setup-gradle)
- [Creating commands](#creating-commands)
  - [Basic slash command](#basic-slash-command)
  - [Sub-commands and permissions](#sub-commands-and-permissions)
  - [Sub-command groups](#sub-command-groups)
  - [Preconditions (@Require)](#preconditions-require)
  - [Auto-complete](#auto-complete)
  - [Buttons and Modals](#buttons-and-modals)
  - [User and Message context commands](#user-and-message-context-commands)
  - [Rate limiting](#rate-limiting)
  - [Custom option names](#custom-option-names)
- [Building the client](#building-the-client)
- [Context Actions](#context-actions)

---

## Requirements

| Dependency         | Version  |
|--------------------|----------|
| Kotlin             | `2.3.0`  |
| KSP                | `2.3.0-1.0.31` |
| JDA                | `6.3.1+` |
| Kotlinx Coroutines | `1.10.2` |
| Java JDK           | `11`     |

---

## Setup (Gradle)

```kotlin
// settings.gradle.kts — enable the KSP plugin
plugins {
    id("com.google.devtools.ksp") version "2.3.0-1.0.31" apply false
}
```

```kotlin
// your-module/build.gradle.kts
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    // Runtime library
    implementation("io.github.blad3mak3r.slash:slash-core:VERSION")

    // Annotations (SOURCE retention — no bytecode impact)
    implementation("io.github.blad3mak3r.slash:slash-annotations:VERSION")

    // KSP processor — generates the wiring code at compile time
    ksp("io.github.blad3mak3r.slash:slash-ksp-processor:VERSION")
}
```

---

## Creating commands

### Basic slash command

```kotlin
@ApplicationCommand("ping")
class PingCommand {

    @OnSlashCommand(target = InteractionTarget.ALL)
    suspend fun default(ctx: SlashCommandContext) {
        ctx.acknowledge(true)
        val restPing    = ctx.jda.restPing.await()
        val gatewayPing = ctx.jda.gatewayPing

        ctx.embed {
            setTitle("Pong!")
            addField("REST",    "${restPing}ms",    true)
            addField("Gateway", "${gatewayPing}ms", true)
        }.queue()
    }
}
```

### Sub-commands and permissions

```kotlin
@ApplicationCommand("role")
class RoleCommand {

    // path: role/add
    @OnSlashCommand(name = "add", target = InteractionTarget.GUILD)
    @Permissions([Permission.MANAGE_ROLES])
    suspend fun addRole(ctx: GuildSlashCommandContext, member: Member) {
        // add role
    }

    // path: role/remove
    @OnSlashCommand(name = "remove", target = InteractionTarget.GUILD)
    @Permissions([Permission.MANAGE_ROLES])
    suspend fun removeRole(ctx: GuildSlashCommandContext, member: Member) {
        // remove role
    }

    // path: role/list — nullable param means the option is optional
    @OnSlashCommand(name = "list", target = InteractionTarget.GUILD)
    suspend fun listRoles(ctx: GuildSlashCommandContext, member: Member?) { }
}
```

### Sub-command groups

```kotlin
@ApplicationCommand("twitch")
class TwitchCommand {

    // path: twitch/clips/top
    @OnSlashCommand(group = "clips", name = "top", target = InteractionTarget.ALL)
    @Permissions([Permission.MESSAGE_EMBED_LINKS], target = PermissionTarget.BOT)
    suspend fun clipTop(ctx: SlashCommandContext, channel: String?) { }

    // path: twitch/clips/random
    @OnSlashCommand(group = "clips", name = "random", target = InteractionTarget.ALL)
    suspend fun clipRandom(ctx: SlashCommandContext, channel: String?) { }
}
```

### Preconditions (`@Require`)

Implement `Precondition` in your project; it runs before the handler executes:

```kotlin
class AdminOnly : Precondition {
    override suspend fun check(ctx: SlashCommandContext): Boolean {
        if (ctx !is GuildSlashCommandContext) return false
        val isAdmin = ctx.member.hasPermission(Permission.ADMINISTRATOR)
        if (!isAdmin) ctx.replyMessage("Admins only.").setEphemeral(true).queue()
        return isAdmin
    }
}

@ApplicationCommand("admin")
@Require(AdminOnly::class)   // applied to every handler in this class
class AdminCommand {

    @OnSlashCommand(name = "ban", target = InteractionTarget.GUILD)
    suspend fun ban(ctx: GuildSlashCommandContext, member: Member) { }
}
```

### Auto-complete

```kotlin
@ApplicationCommand("search")
class SearchCommand {

    @OnSlashCommand(target = InteractionTarget.ALL)
    suspend fun default(ctx: SlashCommandContext, query: String) { }

    // Provides completions for the 'query' option of the default handler
    @OnAutoComplete(option = "query")
    suspend fun queryComplete(ctx: AutoCompleteContext) {
        ctx.replyChoiceStrings(listOf("option a", "option b", "option c")).queue()
    }
}
```

### Buttons and Modals

```kotlin
@ApplicationCommand("demo")
class DemoCommand {

    @OnSlashCommand(target = InteractionTarget.ALL)
    suspend fun showButton(ctx: SlashCommandContext) {
        ctx.replyMessage("Click me!").addActionRow(
            Button.primary("demo:click:${ctx.user.id}", "Click")
        ).queue()
    }

    // pattern is a regex matched against the button's custom ID
    @OnButton("demo:click:(.+?)")
    suspend fun onClick(ctx: ButtonContext) {
        val userId = ctx.matcher.group(1)
        ctx.reply("Clicked by $userId").queue()
    }

    @OnModal("feedback:(.+?)")
    suspend fun onFeedback(ctx: ModalContext) {
        val category = ctx.matcher.group(1)
        ctx.reply("Feedback for $category received.").queue()
    }
}
```

### User and Message context commands

```kotlin
@ApplicationCommand("Get Info", type = CommandType.USER)
class GetUserInfoCommand {

    @OnUserCommand
    suspend fun handle(ctx: UserCommandContext) {
        ctx.reply("User: ${ctx.target.effectiveName}").setEphemeral(true).queue()
    }
}

@ApplicationCommand("Quote", type = CommandType.MESSAGE)
class QuoteCommand {

    @OnMessageCommand
    suspend fun handle(ctx: MessageCommandContext) {
        ctx.reply("Quoted: ${ctx.target.contentDisplay}").setEphemeral(true).queue()
    }
}
```

### Rate limiting

```kotlin
@ApplicationCommand("slow")
class SlowCommand {

    @OnSlashCommand(target = InteractionTarget.GUILD)
    @RateLimit(limit = 3, period = 10_000L)  // 3 calls per 10 seconds
    suspend fun default(ctx: GuildSlashCommandContext) {
        ctx.message("This command is rate limited.").queue()
    }
}
```

### Custom option names

```kotlin
@OnSlashCommand(target = InteractionTarget.ALL)
suspend fun search(ctx: SlashCommandContext, @OptionName("query") q: String) {
    // q maps to the Discord option named "query"
}
```

---

## Building the client

No package name required — all handlers are discovered via `ServiceLoader` from the KSP-generated `META-INF/services` file.

```kotlin
val jda = JDABuilder.createDefault(TOKEN).build()

val client = SlashCommandClient.builder()
    .withTimeout(30.seconds)
    // optional: global interceptors
    .addSlashInterceptor { ctx ->
        val allowed = !isMaintenanceMode()
        if (!allowed) ctx.replyMessage("Bot is in maintenance.").setEphemeral(true).queue()
        allowed
    }
    .buildWith(jda)  // registers as JDA EventListener automatically
```

---

## Context Actions

Every `SlashCommandContext` exposes DSL helpers that automatically choose between `reply()` and `send()`:

```kotlin
@OnSlashCommand(target = InteractionTarget.ALL)
suspend fun contextActions(ctx: SlashCommandContext) {

    // Builds an embed reply/send action
    ctx.embed {
        setTitle("Hello!")
        setDescription("This uses the DSL.")
    }.queue()                          // auto-selects reply vs send

    ctx.message("Plain text reply").queue(ephemeral = true)

    // You can also use the explicit methods
    ctx.replyEmbed { setTitle("Explicit reply") }.setEphemeral(true).queue()
    ctx.sendMessage("Follow-up message").queue()
}
```
