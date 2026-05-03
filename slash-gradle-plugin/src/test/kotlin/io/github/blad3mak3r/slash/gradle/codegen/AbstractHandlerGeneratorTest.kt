package io.github.blad3mak3r.slash.gradle.codegen

import io.github.blad3mak3r.slash.dsl.*
import org.junit.Assert.*
import org.junit.Test

class AbstractHandlerGeneratorTest {

    // ── simple command with top-level options ─────────────────────────────────

    @Test
    fun `generates interface for GUILD command with required String option`() {
        val def = simpleCommand("ping", "Ping the bot", CommandTarget.GUILD) {
            add(OptionDef("message", "Your message", String::class, required = true, choices = emptyList()))
        }

        val code = AbstractHandlerGenerator.generate(def).toString()

        assertTrue("interface PingCommand", code.contains("interface PingCommand"))
        assertTrue("extends SlashCommandHandler", code.contains(": SlashCommandHandler"))
        assertTrue("has onPing method", code.contains("fun onPing("))
        // required String → non-nullable
        assertTrue("message is non-nullable String", code.contains("message: String") && !code.contains("message: String?"))
        assertTrue("GUILD command uses GuildSlashCommandContext", code.contains("GuildSlashCommandContext"))
        assertTrue("has buildCommandData", code.contains("fun buildCommandData()"))
        assertTrue("returns SlashCommandData", code.contains("SlashCommandData"))
        assertTrue("command name literal", code.contains("\"ping\""))
        assertTrue("setGuildOnly(true)", code.contains("setGuildOnly(true)"))
        // dispatch extracts option and calls onPing
        assertTrue("dispatch calls getOption", code.contains("getOption(\"message\")"))
        assertTrue("dispatch calls onPing", code.contains("onPing("))
        // KDoc comment on onPing
        assertTrue("KDoc path comment", code.contains("/ping"))
    }

    @Test
    fun `generates nullable parameter for optional option`() {
        val def = simpleCommand("greet", "Greeter", CommandTarget.ALL) {
            add(OptionDef("name", "Name", String::class, required = false, choices = emptyList()))
        }

        val code = AbstractHandlerGenerator.generate(def).toString()

        assertTrue("optional String is nullable", code.contains("name: String?"))
    }

    // ── subcommands ───────────────────────────────────────────────────────────

    @Test
    fun `generates handler methods for each subcommand`() {
        val def = CommandDef(
            name = "ban",
            description = "Ban management",
            target = CommandTarget.GUILD,
            subcommands = listOf(
                SubcommandDef(
                    name = "member",
                    description = "Ban a member",
                    options = listOf(
                        OptionDef("reason", "Reason", String::class, required = false, choices = emptyList())
                    ),
                    autocompletes = emptyList()
                ),
                SubcommandDef(
                    name = "bot",
                    description = "Ban a bot",
                    options = emptyList(),
                    autocompletes = emptyList()
                )
            ),
            subcommandGroups = emptyList(),
            options = emptyList(),
            buttons = emptyList(),
            modals = emptyList(),
            autocompletes = emptyList()
        )

        val code = AbstractHandlerGenerator.generate(def).toString()

        assertTrue("interface name", code.contains("interface BanCommand"))
        assertTrue("onMember method", code.contains("fun onMember("))
        assertTrue("reason param is nullable", code.contains("reason: String?"))
        assertTrue("onBot method", code.contains("fun onBot("))
        // dispatch uses when block
        assertTrue("dispatch has when", code.contains("when ("))
        assertTrue("dispatch routes member", code.contains("\"member\""))
        assertTrue("dispatch routes bot", code.contains("\"bot\""))
        // KDoc path comments
        assertTrue("KDoc for onMember", code.contains("/ban member"))
        assertTrue("KDoc for onBot", code.contains("/ban bot"))
    }

    // ── buttons and modals ────────────────────────────────────────────────────

    @Test
    fun `generates button and modal handlers with companion regex constants`() {
        val def = CommandDef(
            name = "ban",
            description = "Ban management",
            target = CommandTarget.GUILD,
            subcommands = emptyList(),
            subcommandGroups = emptyList(),
            options = emptyList(),
            buttons = listOf(ButtonDef("ban-confirm-[a-z0-9]+")),
            modals = listOf(ModalDef("ban-appeal")),
            autocompletes = emptyList()
        )

        val code = AbstractHandlerGenerator.generate(def).toString()

        assertTrue("dispatchButton override", code.contains("override suspend fun dispatchButton("))
        assertTrue("dispatchModal override", code.contains("override suspend fun dispatchModal("))
        // handler methods
        assertTrue("button handler", code.contains("Button("))
        assertTrue("modal handler", code.contains("Modal("))
        // companion regex constants
        assertTrue("BTN_REGEX constant in companion", code.contains("BTN_REGEX"))
        assertTrue("MODAL_REGEX constant in companion", code.contains("MODAL_REGEX"))
        assertTrue("Regex(\"ban-confirm", code.contains("Regex(\"ban-confirm"))
        assertTrue("Regex(\"ban-appeal", code.contains("Regex(\"ban-appeal"))
        // KDoc comments on button/modal handlers
        assertTrue("KDoc for button", code.contains("ban-confirm-[a-z0-9]+"))
        assertTrue("KDoc for modal", code.contains("ban-appeal"))
    }

    // ── DM target ─────────────────────────────────────────────────────────────

    @Test
    fun `DM target command uses SlashCommandContext and does not set guildOnly`() {
        val def = simpleCommand("help", "Get help", CommandTarget.DM) {
            add(OptionDef("topic", "Topic", String::class, required = false, choices = emptyList()))
        }

        val code = AbstractHandlerGenerator.generate(def).toString()

        assertFalse("should NOT use GuildSlashCommandContext", code.contains("GuildSlashCommandContext"))
        assertFalse("should NOT call setGuildOnly(true)", code.contains("setGuildOnly(true)"))
        assertTrue("should use SlashCommandContext", code.contains("SlashCommandContext"))
    }

    // ── type mappings ─────────────────────────────────────────────────────────

    @Test
    fun `type mappings generate correct JDA OptionType constants`() {
        val def = simpleCommand("test", "Types test", CommandTarget.ALL) {
            add(OptionDef("str",  "string",  String::class,  required = true, choices = emptyList()))
            add(OptionDef("num",  "integer", Int::class,     required = true, choices = emptyList()))
            add(OptionDef("lng",  "long",    Long::class,    required = true, choices = emptyList()))
            add(OptionDef("dbl",  "double",  Double::class,  required = true, choices = emptyList()))
            add(OptionDef("flag", "boolean", Boolean::class, required = true, choices = emptyList()))
        }

        val code = AbstractHandlerGenerator.generate(def).toString()

        assertTrue("STRING option type", code.contains("OptionType.STRING"))
        assertTrue("INTEGER option type", code.contains("OptionType.INTEGER"))
        assertTrue("NUMBER option type", code.contains("OptionType.NUMBER"))
        assertTrue("BOOLEAN option type", code.contains("OptionType.BOOLEAN"))
    }

    @Test
    fun `type mappings use correct accessors`() {
        val def = simpleCommand("test", "Accessor test", CommandTarget.ALL) {
            add(OptionDef("s", "s", String::class,  required = true, choices = emptyList()))
            add(OptionDef("i", "i", Int::class,     required = true, choices = emptyList()))
            add(OptionDef("d", "d", Double::class,  required = true, choices = emptyList()))
            add(OptionDef("b", "b", Boolean::class, required = true, choices = emptyList()))
        }

        val code = AbstractHandlerGenerator.generate(def).toString()

        assertTrue(".asString()", code.contains(".asString()"))
        assertTrue(".asInt()", code.contains(".asInt()"))
        assertTrue(".asDouble()", code.contains(".asDouble()"))
        assertTrue(".asBoolean()", code.contains(".asBoolean()"))
    }

    // ── PascalCase naming ─────────────────────────────────────────────────────

    @Test
    fun `kebab-case command name becomes PascalCase interface and method name`() {
        val def = simpleCommand("slash-command", "Kebab test", CommandTarget.GUILD)

        val code = AbstractHandlerGenerator.generate(def).toString()

        assertTrue("interface uses PascalCase", code.contains("interface SlashCommandCommand"))
        assertTrue("getCommandName returns kebab original", code.contains("\"slash-command\""))
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun simpleCommand(
        name: String,
        description: String,
        target: CommandTarget,
        options: MutableList<OptionDef<*>>.() -> Unit = {}
    ) = CommandDef(
        name = name,
        description = description,
        target = target,
        subcommands = emptyList(),
        subcommandGroups = emptyList(),
        options = buildList { options() },
        buttons = emptyList(),
        modals = emptyList(),
        autocompletes = emptyList()
    )
}
