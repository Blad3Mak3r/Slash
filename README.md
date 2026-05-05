[maven-central]: https://central.sonatype.com/search?q=io.github.blad3mak3r.slash
[kotlin]: https://kotlinlang.org/
[jda]: https://github.com/discord-jda/JDA
[ksp]: https://github.com/google/ksp
[slash-commands]: https://discord.com/developers/docs/interactions/application-commands

# Slash

[![Maven Central](https://img.shields.io/maven-central/v/io.github.blad3mak3r.slash/slash-core?color=blue&label=Maven%20Central)][maven-central]
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)][kotlin]
[![JDA](https://img.shields.io/badge/JDA-6.3.1-5865F2)](https://github.com/discord-jda/JDA)
[![KSP2](https://img.shields.io/badge/KSP-2.3.7-4285F4)][ksp]
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/Blad3Mak3r/Slash)

**Zero-reflection Discord Application Command framework for Kotlin/JDA, powered by KSP2.**

Slash turns annotated Kotlin classes into fully-wired JDA interaction handlers. All routing code is generated at **compile time** by a [KSP2][ksp] processor — no classpath scanning, no `kotlin-reflect`, no startup overhead.

---

## Why Slash?

| | Reflection-based routing | Slash + KSP2 |
|---|---|---|
| **Startup** | Scans classpath, reflects every class | Instant — `ServiceLoader` loads pre-generated registrars |
| **Runtime overhead** | Reflects on every interaction | Zero — plain Kotlin function calls |
| **`kotlin-reflect`** | Required | Not needed |
| **Error detection** | `NoSuchMethodException` at runtime | Compile error |
| **GraalVM Native** | Complex `reflect-config.json` required | Works out of the box |
| **Bytecode impact** | Annotations retained in bytecode | `SOURCE` retention — erased after compilation |

---

## Features

- **Compile-time code generation** via KSP2 — zero reflection at runtime
- **Coroutines-first** — all handlers are `suspend fun`; built on structured concurrency
- **Type-safe option injection** — Kotlin parameter types map directly to Discord option types
- **21 channel types supported** — from `GuildChannel` to `ForumChannel`, nullable or non-null
- **Subcommands & groups** — nested paths via `group`/`name` fields on `@OnSlashCommand`
- **Auto-complete** — `@OnAutoComplete` with typed parameter injection, just like slash handlers
- **Buttons & Modals** — regex pattern matching on `customId`; capture groups via `ctx.matcher`
- **Context menus** — `CommandType.USER` and `CommandType.MESSAGE`
- **Preconditions** — reusable, composable guards at class and/or function level
- **Permissions** — user and bot permission checks declared as annotations
- **Rate limiting** — per-user sliding-window bucketing via pluggable `RateLimitClient`
- **Interceptors** — before-handler middleware for cross-cutting concerns
- **ShardManager support** — first-class multi-shard bot support

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Command Types](#command-types)
  - [Slash Commands](#slash-commands)
  - [Subcommands and Groups](#subcommands-and-groups)
  - [Context Menu Commands](#context-menu-commands)
- [Option Types](#option-types)
- [Auto-Complete](#auto-complete)
- [Buttons and Modals](#buttons-and-modals)
- [Permissions](#permissions)
- [Preconditions](#preconditions)
- [Rate Limiting](#rate-limiting)
- [Interceptors](#interceptors)
- [Building the Client](#building-the-client)
- [Context Actions DSL](#context-actions-dsl)
- [How It Works](#how-it-works)

---

## Requirements

| Dependency         | Version  |
|--------------------|----------|
| Kotlin             | `2.0+`   |
| KSP                | `2.x`    |
| JDA                | `6.x`    |
| Kotlinx Coroutines | `1.9+`   |
| Java               | `11+`    |
| Gradle             | `8+`     |

---

## Installation

Slash is distributed as three artifacts. In practice you only need to declare two of them — `slash-annotations` is pulled transitively through `slash-core`.

### 1. Version Catalog (`gradle/libs.versions.toml`)

```toml
[versions]
slash = "0.20.0-alpha.4"
ksp   = "2.3.7"           # must match your Kotlin plugin version exactly

[libraries]
slash-core          = { module = "io.github.blad3mak3r.slash:slash-core",          version.ref = "slash" }
slash-ksp-processor = { module = "io.github.blad3mak3r.slash:slash-ksp-processor", version.ref = "slash" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

> The KSP version must match your Kotlin version. Check the [KSP releases](https://github.com/google/ksp/releases) page for the correct pairing (e.g. Kotlin `2.3.20` → KSP `2.3.20-...`).

### 2. Root Build File (`build.gradle.kts`)

```kotlin
plugins {
    alias(libs.plugins.ksp) apply false   // declare once, don't apply at root
}
```

### 3. Module Build File (`app/build.gradle.kts`)

```kotlin
plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.slash.core)
    ksp(libs.slash.ksp.processor)

    // JDA (required at runtime — not included transitively)
    implementation("net.dv8tion:JDA:6.3.1") { exclude(module = "opus-java") }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}
```

> **Multi-module projects:** the `ksp(...)` declaration must be present in every module that defines `@ApplicationCommand` classes. Each module generates its own `META-INF/services` file.

### 4. IntelliJ IDEA

Delegate build and run actions to Gradle so the KSP processor runs on every build:

`Settings → Build, Execution, Deployment → Build Tools → Gradle → Build and run using: **Gradle**`

---

## Quick Start

### 1. Define a command

```kotlin
import io.github.blad3mak3r.slash.annotations.ApplicationCommand
import io.github.blad3mak3r.slash.annotations.OnSlashCommand
import io.github.blad3mak3r.slash.context.SlashCommandContext

@ApplicationCommand(name = "greet")
class GreetCommand {

    @OnSlashCommand
    suspend fun handle(ctx: SlashCommandContext, name: String, times: Long?) {
        val count = times ?: 1L
        ctx.replyMessage("Hello, $name! (×$count)").queue()
    }
}
```

KSP generates a `GreetCommandRegistrar` at compile time. At runtime, `ServiceLoader` discovers it automatically — no manual registration required.

### 2. Build the client and wire it to JDA

```kotlin
val jda = JDABuilder.createDefault(TOKEN).build()

val client = SlashCommandClient.builder()
    .buildWith(jda)   // auto-registers as a JDA EventListener
```

That's all. The framework handles the rest.

---

## Command Types

### Slash Commands

Annotate a class with `@ApplicationCommand` and each handler method with `@OnSlashCommand`. The `target` field controls where the command can be used.

```kotlin
@ApplicationCommand(name = "ban")
@Permissions([Permission.BAN_MEMBERS])    // required user permissions (class-wide)
@RateLimit(limit = 3, period = 60_000L)   // 3 uses per 60 s (class-wide)
@Require(AdminOnly::class)                // custom precondition (class-wide)
class BanCommand {

    @OnSlashCommand(target = InteractionTarget.GUILD)
    suspend fun handle(
        ctx: GuildSlashCommandContext,  // narrowed context — guild, member are non-null
        member: Member,
        reason: String?                 // nullable = optional Discord option
    ) {
        ctx.guild.ban(member, 0, TimeUnit.SECONDS).reason(reason).queue()
        ctx.replyMessage("${member.user.name} was banned.").setEphemeral(true).queue()
    }
}
```

`InteractionTarget` controls scope:

| Value | Context type | Description |
|---|---|---|
| `GUILD` (default) | `GuildSlashCommandContext` | Guild channels only; `guild` and `member` are non-null |
| `DM` | `SlashCommandContext` | Direct messages only |
| `ALL` | `SlashCommandContext` | Both guilds and DMs |

### Subcommands and Groups

Use `name` to define a subcommand (`/stats user`), and both `group` + `name` for a nested group (`/stats server info`):

```kotlin
@ApplicationCommand(name = "stats")
class StatsCommand {

    // /stats user
    @OnSlashCommand(name = "user")
    suspend fun userStats(ctx: SlashCommandContext, user: User?) { ... }

    // /stats server info
    @OnSlashCommand(group = "server", name = "info")
    suspend fun serverInfo(ctx: SlashCommandContext) { ... }

    // /stats server members
    @OnSlashCommand(group = "server", name = "members")
    suspend fun serverMembers(ctx: SlashCommandContext) { ... }
}
```

### Context Menu Commands

```kotlin
@ApplicationCommand(name = "Get Avatar", type = CommandType.USER)
class GetAvatarCommand {

    @OnUserCommand
    suspend fun handle(ctx: UserCommandContext) {
        ctx.reply("Avatar: ${ctx.target.effectiveAvatarUrl}").setEphemeral(true).queue()
    }
}

@ApplicationCommand(name = "Quote", type = CommandType.MESSAGE)
class QuoteCommand {

    @OnMessageCommand
    suspend fun handle(ctx: MessageCommandContext) {
        ctx.reply("📌 *\"${ctx.target.contentRaw}\"*").setEphemeral(true).queue()
    }
}
```

---

## Option Types

Kotlin parameter types are mapped automatically to Discord option types. **Nullable parameters (`Type?`) become optional Discord options.**

| Kotlin type | Discord option type | Notes |
|---|---|---|
| `String` | `STRING` | |
| `Long` | `INTEGER` | |
| `Int` | `INTEGER` | |
| `Boolean` | `BOOLEAN` | |
| `Double` | `NUMBER` | |
| `Float` | `NUMBER` | Mapped via `asDouble.toFloat()` |
| `Member` | `USER` | Non-null (guild only) |
| `User` | `USER` | |
| `Role` | `ROLE` | |
| `Message.Attachment` | `ATTACHMENT` | |
| `IMentionable` | `MENTIONABLE` | |
| `GuildChannel` | `CHANNEL` | Any guild channel; no cast needed |
| `GuildMessageChannel` | `CHANNEL` | Text + news channels |
| `AudioChannel` | `CHANNEL` | Voice + stage channels |
| `TextChannel` | `CHANNEL` | Text channels only |
| `VoiceChannel` | `CHANNEL` | Voice channels only |
| `StageChannel` | `CHANNEL` | Stage channels only |
| `NewsChannel` | `CHANNEL` | Announcement channels |
| `Category` | `CHANNEL` | Category containers |
| `ThreadChannel` | `CHANNEL` | Thread channels |
| `ForumChannel` | `CHANNEL` | Forum channels |
| `MediaChannel` | `CHANNEL` | Media channels |

### Custom Option Names

By default the Discord option name matches the Kotlin parameter name. Use `@OptionName` to override:

```kotlin
@OnSlashCommand(name = "search")
suspend fun search(ctx: SlashCommandContext, @OptionName("q") query: String) { ... }
// Discord option: "q" — Kotlin variable: query
```

---

## Auto-Complete

`@OnAutoComplete` handlers can receive typed parameters — KSP resolves and injects them at compile time, the same way it does for slash handlers:

```kotlin
@ApplicationCommand(name = "color")
class ColorCommand {

    @OnSlashCommand
    suspend fun handle(ctx: SlashCommandContext, color: String) {
        ctx.replyMessage("You chose: $color").queue()
    }

    @OnAutoComplete(option = "color")
    suspend fun complete(ctx: AutoCompleteContext, color: String) {
        // `color` is injected from the focused option value by KSP
        val choices = listOf("Red", "Green", "Blue", "Yellow", "Purple")
            .filter { it.startsWith(color, ignoreCase = true) }
        ctx.replyChoiceStrings(choices).queue()
    }
}
```

For subcommand handlers, set `group` and `name` on `@OnAutoComplete` to match the handler:

```kotlin
@OnAutoComplete(group = "clips", name = "top", option = "channel")
suspend fun completeChannel(ctx: AutoCompleteContext, channel: String) { ... }
```

---

## Buttons and Modals

Use regex patterns to route button and modal interactions. Capture groups are accessible via `ctx.matcher`:

```kotlin
@ApplicationCommand(name = "feedback")
class FeedbackCommand {

    @OnSlashCommand
    suspend fun handle(ctx: SlashCommandContext) {
        ctx.replyMessage("Click below to send feedback:")
            .addActionRow(Button.primary("feedback:open:${ctx.user.id}", "Send Feedback"))
            .queue()
    }

    // Matches "feedback:open:<userId>" and captures the user ID
    @OnButton(pattern = "feedback:open:([0-9]+)")
    suspend fun onButton(ctx: ButtonContext) {
        val userId = ctx.matcher.group(1)
        ctx.replyModal(
            customId = "feedback:modal:$userId",
            title    = "Feedback"
        ) {
            short("content", "Your message", required = true)
        }.queue()
    }

    @OnModal(pattern = "feedback:modal:[0-9]+")
    suspend fun onModal(ctx: ModalContext) {
        val text = ctx.getValue("content")?.asString ?: "(empty)"
        ctx.reply("Thanks! We received: $text").setEphemeral(true).queue()
    }
}
```

---

## Permissions

Declare required Discord permissions at class level (all handlers) or function level (single handler). Use `PermissionTarget.BOT` to check the bot's own permissions instead of the user's:

```kotlin
@ApplicationCommand(name = "setup")
class SetupCommand {

    @OnSlashCommand(group = "greetings", name = "preview")
    @Permissions([Permission.MESSAGE_EMBED_LINKS], PermissionTarget.BOT)  // bot needs embed links
    suspend fun greetingsPreview(ctx: GuildSlashCommandContext) { ... }

    @OnSlashCommand(group = "greetings", name = "set")
    @Permissions([Permission.MANAGE_GUILD])                               // user needs manage guild
    suspend fun greetingsSet(ctx: GuildSlashCommandContext, channel: TextChannel) { ... }
}
```

---

## Preconditions

Implement `Precondition` to create reusable guards. Return `false` to abort the handler silently (handle the reply inside the precondition itself):

```kotlin
class AdminOnly : Precondition {
    override suspend fun check(ctx: SlashCommandContext): Boolean {
        val isAdmin = ctx.member?.hasPermission(Permission.ADMINISTRATOR) == true
        if (!isAdmin) ctx.replyMessage("This command is for admins only.").setEphemeral(true).queue()
        return isAdmin
    }
}
```

Apply with `@Require` at class level, function level, or both — all collected preconditions are evaluated before the handler runs:

```kotlin
@ApplicationCommand(name = "admin")
@Require(AdminOnly::class)                 // runs for every handler in this class
class AdminCommand {

    @OnSlashCommand(name = "kick")
    @Require(NotOnCooldown::class)         // runs alongside AdminOnly for this handler only
    suspend fun kick(ctx: GuildSlashCommandContext, member: Member) { ... }

    @OnSlashCommand(name = "mute")
    suspend fun mute(ctx: GuildSlashCommandContext, member: Member) { ... }  // only AdminOnly
}
```

---

## Rate Limiting

Declare a rate limit at class level (default for all handlers) or function level (overrides class default). The `period` is in **milliseconds**:

```kotlin
@ApplicationCommand(name = "search")
@RateLimit(limit = 5, period = 60_000L)   // 5 requests per minute, class-wide default
class SearchCommand {

    @OnSlashCommand
    suspend fun search(ctx: SlashCommandContext, query: String) { ... }

    @OnSlashCommand(name = "image")
    @RateLimit(limit = 2, period = 60_000L)   // stricter override for this handler
    suspend fun searchImage(ctx: SlashCommandContext, query: String) { ... }
}
```

Rate limiting requires a `RateLimitClient` implementation. The framework provides the interface and config; you supply the storage backend (in-memory, Redis, etc.):

```kotlin
class InMemoryRateLimitClient : RateLimitClient {

    private val buckets = ConcurrentHashMap<String, ArrayDeque<Long>>()

    override suspend fun acquire(config: RateLimitConfig, event: SlashCommandInteractionEvent): Long? {
        val key = createBucketKey(config, event)  // default: "limit:user:<id>:<path>"
        val now = System.currentTimeMillis()
        val window = buckets.getOrPut(key) { ArrayDeque() }

        // Drop timestamps outside the window
        while (window.isNotEmpty() && now - window.first() > config.period) {
            window.removeFirst()
        }

        return if (window.size < config.limit) {
            window.addLast(now)
            null                                         // allowed
        } else {
            config.period - (now - window.first())       // ms until next slot
        }
    }
}
```

Override `createBucketKey` to change the bucketing strategy (e.g. per-guild instead of per-user), and `onRateLimitHit` to customize the reply:

```kotlin
class GuildRateLimitClient : InMemoryRateLimitClient() {
    override fun createBucketKey(config: RateLimitConfig, event: SlashCommandInteractionEvent) =
        "limit:guild:${event.guild?.id}:${event.commandPath}"
}
```

Register it on the builder:

```kotlin
SlashCommandClient.builder()
    .setRateLimitClient(InMemoryRateLimitClient())
    .buildWith(jda)
```

---

## Interceptors

Interceptors run before every interaction of a given type. Return `false` to abort — the framework will not invoke the handler:

```kotlin
SlashCommandClient.builder()
    .addSlashInterceptor { ctx ->
        if (maintenanceMode) {
            ctx.replyMessage("Bot is under maintenance.").setEphemeral(true).queue()
            return@addSlashInterceptor false
        }
        true
    }
    .addSlashInterceptor { ctx ->
        logger.info("/${ctx.interaction.commandPath} by ${ctx.user.name}")
        true  // always allow, just log
    }
    .buildWith(jda)
```

Typed class-based interceptors are also supported via `SlashCommandInterceptor`, `UserCommandInterceptor`, and `MessageCommandInterceptor`.

---

## Building the Client

```kotlin
val client = SlashCommandClient.builder()
    .setRateLimitClient(myRateLimitClient)
    .setExceptionHandler { ctx, throwable ->
        logger.error("Error in ${ctx?.interaction?.name}", throwable)
    }
    .addSlashInterceptor { ctx -> !maintenanceMode }
    .withTimeout(2.minutes)
    .buildWith(jda)              // registers as EventListener automatically
```

### ShardManager

```kotlin
val client = SlashCommandClient.builder()
    .buildWith(shardManager)     // works the same as buildWith(jda)
```

### Manual EventListener

```kotlin
val client = SlashCommandClient.builder().build()
jda.addEventListener(client)
```

---

## Context Actions DSL

`SlashCommandContext` exposes DSL helpers that **automatically select `reply()` or `send()`** depending on whether the interaction has already been acknowledged:

```kotlin
@OnSlashCommand
suspend fun handle(ctx: SlashCommandContext) {

    // Embed — auto reply or send
    ctx.embed {
        setTitle("Hello!")
        setDescription("Uses the DSL.")
    }.queue()

    // Plain text — ephemeral
    ctx.message("Something went wrong.").setEphemeral(true).queue()

    // Explicit control
    ctx.replyEmbed { setTitle("Explicit reply") }.setEphemeral(true).queue()
    ctx.sendMessage("A follow-up message after acknowledge.").queue()
}
```

Use `acknowledge()` before any long-running work to avoid the 3-second Discord timeout:

```kotlin
@OnSlashCommand
suspend fun handle(ctx: SlashCommandContext) {
    ctx.acknowledge(ephemeral = true)       // defers the reply

    val data = fetchFromDatabase()          // can take a while

    ctx.sendEmbed {
        setTitle("Result")
        setDescription(data.toString())
    }.queue()
}
```

---

## How It Works

```
@ApplicationCommand class BanCommand
         │
         ▼  KSP2 — compile time
BanCommandRegistrar.kt (generated) ──► META-INF/services/...CommandRegistrar
         │
         ▼  java.util.ServiceLoader — startup
HandlerRegistry["ban"] = SlashEntry(handler = { ... })
         │
         ▼  JDA fires SlashCommandInteractionEvent("ban")
SlashCommandClient ──► permissions ──► preconditions ──► rate limit ──► handler(ctx)
```

1. **Compile time** — KSP2 reads every `@ApplicationCommand` class and generates a `*Registrar.kt` file plus a `META-INF/services` manifest. No reflection is emitted.
2. **Startup** — `ServiceLoader` discovers every registrar on the classpath and populates the `HandlerRegistry` with pre-built lambda functions.
3. **Runtime** — Each JDA event is routed in **O(1)** via a `HashMap` lookup. No reflection. No scanning. Just a function call.

---

## License

```
Copyright 2024 Blad3Mak3r

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```

See [LICENSE](LICENSE) for the full text.
