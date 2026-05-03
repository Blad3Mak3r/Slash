[maven-central-shield]: https://img.shields.io/maven-central/v/io.github.blad3mak3r/slash?color=blue
[maven-central]: https://search.maven.org/artifact/io.github.blad3mak3r/slash
[kotlin]: https://kotlinlang.org/
[jda]: https://github.com/DV8FromTheWorld/JDA
[slash-commands]: https://discord.com/developers/docs/interactions/application-commands

# Slash [![Maven Central][maven-central-shield]][maven-central]

A **[Kotlin][kotlin]** library for **[JDA][jda]** that implements **[Discord Application Commands][slash-commands]** via **compile-time code generation** — no reflection at runtime.

You define commands with a Kotlin DSL; the Gradle plugin generates type-safe abstract handler classes at build time. You implement the handlers; the plugin wires everything together into a `SlashCommandRegistry`.

## How it works

```
src/slash/kotlin/Commands.kt   (your DSL definitions)
         │
         ▼  Pass 1: processSlashDefs
Abstract*CommandHandler.kt     (generated — build/generated/slash/handlers/)
         │
         ▼  you implement these
BanCommandHandler.kt, PingCommandHandler.kt, …
         │
         ▼  Pass 2: generateSlashRegistry  (after compileKotlin)
SlashCommandRegistry.kt        (generated — build/generated/slash/registry/)
         │
         ▼  runtime
SlashCommandClient.builder(SlashCommandRegistry).buildWith(jda)
```

## Requirements

| Dependency           | Version   |
|----------------------|-----------|
| Kotlin               | `2.3.0`   |
| JDA                  | `6.3.1`   |
| Kotlinx Coroutines   | `1.10.2`  |
| Java JDK             | `21`      |

## Setup

Apply the plugin and add the runtime dependency in your `build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    id("io.github.blad3mak3r.slash") version "x.y.z"
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    implementation("io.github.blad3mak3r:slash-runtime:x.y.z")
    compileOnly("net.dv8tion:JDA:6.3.1") { exclude(module = "opus-java") }
}
```

The plugin automatically:
- adds `slash-dsl` to your compile classpath
- creates the `src/slash/kotlin/` source set
- wires `processSlashDefs` before `compileKotlin`
- wires `generateSlashRegistry` after `compileKotlin`

## Step 1 — Define commands

Create Kotlin files inside `src/slash/kotlin/`. Each file can define one or more commands using the `command { }` DSL.

### Simple command

```kotlin
// src/slash/kotlin/Commands.kt
val ping = command("ping") {
    description = "Check bot latency"
    allContexts()              // GUILD + DM; default is guildOnly()
}
```

### Command with options

```kotlin
val whois = command("whois") {
    description = "Show user info"
    guildOnly()
    option<Member>("target") {
        description = "Member to look up"
        required()
    }
}
```

### Command with subcommands

```kotlin
val ban = command("ban") {
    description = "Ban management"
    guildOnly()

    subcommand("member") {
        description = "Ban a member"
        option<User>("target")   { description = "Member to ban"; required() }
        option<String>("reason") { description = "Reason for the ban" }
    }

    subcommand("bot") {
        description = "Ban a bot"
        option<User>("target") { description = "Bot to ban"; required() }
    }
}
```

### Subcommand groups

```kotlin
val twitch = command("twitch") {
    description = "Twitch integration"
    allContexts()

    subcommandGroup("clips") {
        description = "Clip browsing"

        subcommand("top") {
            description = "Top clips"
            option<String>("channel") { description = "Channel name" }
        }

        subcommand("random") {
            description = "Random clip"
            option<String>("channel") { description = "Channel name" }
        }
    }
}
```

### Buttons, modals, and autocomplete

```kotlin
val ban = command("ban") {
    description = "Ban management"
    guildOnly()

    subcommand("member") {
        description = "Ban a member"
        option<User>("target")   { description = "Member to ban"; required() }
        option<String>("reason") { description = "Reason"; required() }
        autocomplete("reason")   // triggers onReasonAutocomplete(ctx)
    }

    button("ban-confirm-[a-z0-9]+")  // regex pattern → onBanConfirmButton(ctx)
    modal("ban-appeal")              // regex pattern → onBanAppealModal(ctx)
}
```

### Option choices

```kotlin
val color = command("color") {
    description = "Pick a color"
    option<String>("name") {
        description = "Color name"
        required()
        choice("Red",   "red")
        choice("Green", "green")
        choice("Blue",  "blue")
    }
}
```

## Step 2 — Build

Run any build task (e.g. `./gradlew compileKotlin`) to trigger code generation. The plugin produces one abstract class per command in `build/generated/slash/handlers/`.

For the `ban` example above the generated class looks like:

```kotlin
// build/generated/slash/handlers/AbstractBanCommandHandler.kt  (generated — do not edit)
abstract class AbstractBanCommandHandler : AbstractCommandHandler() {
    override fun getCommandName() = "ban"
    override fun buildCommandData(): SlashCommandData { /* … */ }

    override suspend fun dispatch(ctx: SlashCommandContext) { /* routes to on* methods */ }
    override suspend fun dispatchButton(ctx: ButtonContext): Boolean { /* regex routing */ }
    override suspend fun dispatchModal(ctx: ModalContext): Boolean { /* regex routing */ }

    abstract suspend fun onMember(ctx: GuildSlashCommandContext, target: User, reason: String?)
    abstract suspend fun onBot(ctx: GuildSlashCommandContext, target: User)

    abstract suspend fun onBanConfirmButton(ctx: ButtonContext)
    abstract suspend fun onBanAppealModal(ctx: ModalContext)

    // Autocomplete handler (if declared)
    open suspend fun onReasonAutocomplete(ctx: AutoCompleteContext) {}

    companion object {
        val BAN_CONFIRM_A_Z0_9__BTN_REGEX = Regex("ban-confirm-[a-z0-9]+")
        val BAN_APPEAL_MODAL_REGEX        = Regex("ban-appeal")
    }
}
```

Key rules the generator follows:
- `guildOnly()` → context parameter is `GuildSlashCommandContext`; otherwise `SlashCommandContext`
- Optional options (no `required()`) → nullable parameter type (`String?`, `User?`, …)
- Button / modal patterns are compiled to `Regex` constants in the companion object

## Step 3 — Implement handlers

Create concrete classes anywhere in `src/main/kotlin/` that extend the generated abstract class:

```kotlin
class BanCommandHandler : AbstractBanCommandHandler() {

    override suspend fun onMember(ctx: GuildSlashCommandContext, target: User, reason: String?) {
        ctx.acknowledge(ephemeral = true)
        // … ban logic …
        ctx.sendMessage("Banned ${target.asMention}${reason?.let { " — $it" } ?: ""}").queue()
    }

    override suspend fun onBot(ctx: GuildSlashCommandContext, target: User) {
        ctx.acknowledge(ephemeral = true)
        // … ban bot logic …
    }

    override suspend fun onBanConfirmButton(ctx: ButtonContext) {
        ctx.reply("Ban confirmed.").setEphemeral(true).queue()
    }

    override suspend fun onBanAppealModal(ctx: ModalContext) {
        ctx.reply("Appeal received.").setEphemeral(true).queue()
    }
}
```

After the next build the plugin generates `SlashCommandRegistry` (an `object` implementing `SlashRegistry`) that includes every concrete handler it finds on the compile classpath.

## Step 4 — Connect to JDA

```kotlin
fun main() {
    val jda = JDABuilder.createDefault(token).build().awaitReady()

    // Register commands with Discord (global)
    SlashCommandRegistry.registerCommandsWith(jda)

    // Register commands to a single guild (faster for testing)
    SlashCommandRegistry.registerCommandsWith(jda, guildId = 123456789L)

    // Attach the event listener
    SlashCommandClient.builder(SlashCommandRegistry).buildWith(jda)
}
```

For shard-manager setups:

```kotlin
val shardManager = DefaultShardManagerBuilder.createDefault(token).build()

SlashCommandRegistry.registerCommandsWith(shardManager)
SlashCommandClient.builder(SlashCommandRegistry).buildWith(shardManager)
```

## Context types

| Class | When received | Notable members |
|---|---|---|
| `SlashCommandContext` | Any slash command | `user`, `options`, `hook`, `isFromGuild`, `acknowledge()`, `replyMessage()`, `sendMessage()`, `replyModal()` |
| `GuildSlashCommandContext` | Guild-only commands | extends above + `guild`, `member` (non-null), `selfMember`, `channel` (GuildMessageChannel) |
| `ButtonContext` | Button interactions | delegates to `ButtonInteraction` |
| `ModalContext` | Modal submissions | delegates to `ModalInteraction` + `acknowledge()` |
| `AutoCompleteContext` | Autocomplete events | delegates to `CommandAutoCompleteInteraction` |

### Acknowledging interactions

Discord requires a response within 3 seconds. Use `acknowledge()` to defer:

```kotlin
override suspend fun onMember(ctx: GuildSlashCommandContext, target: User, reason: String?) {
    ctx.acknowledge()             // defer (public)
    ctx.acknowledge(ephemeral = true)  // defer (ephemeral)

    // … slow work …

    ctx.sendMessage("Done!").queue()
}
```

### Sending responses

```kotlin
// Immediate reply (before acknowledge)
ctx.replyMessage("Pong!").queue()
ctx.replyMessage { setContent("Pong!") }.queue()

// Follow-up (after acknowledge / deferReply)
ctx.sendMessage("Done!").queue()
ctx.sendMessage { setContent("Done!").setEphemeral(true) }.queue()

// Open a modal
ctx.replyModal("my-modal-id", "My Modal") {
    add(TextInput.create("reason", "Reason", TextInputStyle.SHORT).build())
}
```

## Supported option types

| Kotlin type | Discord option type | Accessor |
|---|---|---|
| `String` | `STRING` | `.asString()` |
| `Int` | `INTEGER` | `.asInt()` |
| `Long` | `INTEGER` | `.asLong()` |
| `Double` | `NUMBER` | `.asDouble()` |
| `Boolean` | `BOOLEAN` | `.asBoolean()` |
| `User` | `USER` | `.asUser()` |
| `Member` | `USER` | `.asMember()` |
| `Role` | `ROLE` | `.asRole()` |
| `IMentionable` | `MENTIONABLE` | `.asMentionable()` |
| `GuildChannelUnion` | `CHANNEL` | `.asChannel()` |
| `Message.Attachment` | `ATTACHMENT` | `.asAttachment()` |

## DSL reference

```
command(name) {
    description = "…"
    guildOnly()           // default — GuildSlashCommandContext in handlers
    dmOnly()              // DM only — SlashCommandContext in handlers
    allContexts()         // both   — SlashCommandContext in handlers

    option<T>(name) {
        description = "…"
        required()            // omit for nullable parameter
        choice("Label", value)
    }

    subcommand(name) {
        description = "…"
        option<T>(name) { … }
        autocomplete("optionName")
    }

    subcommandGroup(name) {
        description = "…"
        subcommand(name) { … }
    }

    button("regex-pattern")       // generates onXxxButton(ctx: ButtonContext)
    modal("regex-pattern")        // generates onXxxModal(ctx: ModalContext)
    autocomplete("optionName")    // generates onXxxAutocomplete(ctx: AutoCompleteContext)
}
```

## Download

### Gradle (Kotlin DSL)

```kotlin
plugins {
    id("io.github.blad3mak3r.slash") version "x.y.z"
}

dependencies {
    implementation("io.github.blad3mak3r:slash-runtime:x.y.z")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.blad3mak3r</groupId>
    <artifactId>slash-runtime</artifactId>
    <version>x.y.z</version>
</dependency>
```
