package io.github.blad3mak3r.slash.dsl

import kotlin.reflect.KClass

@DslMarker
annotation class SlashDsl

// ── Entry point ──────────────────────────────────────────────────────────────

/**
 * Defines a slash command and registers it in [CommandRegistry].
 *
 * Usage (in `src/slash/kotlin/`):
 * ```kotlin
 * val ban = command("ban") {
 *     description = "Ban management"
 *     guildOnly()
 *     subcommand("member") {
 *         description = "Ban a member"
 *         option<User>("target") { description = "Member to ban"; required() }
 *         option<String>("reason") { description = "Reason"; required() }
 *     }
 *     button("ban-confirm-[a-z0-9]+")
 *     modal("ban-appeal")
 * }
 * ```
 */
fun command(name: String, block: CommandBuilder.() -> Unit): CommandDef =
    CommandBuilder(name).apply(block).build().also { CommandRegistry.register(it) }

// ── Builders ─────────────────────────────────────────────────────────────────

@SlashDsl
class CommandBuilder internal constructor(private val name: String) {
    var description: String = ""
    private var target: CommandTarget = CommandTarget.GUILD
    private val subcommands = mutableListOf<SubcommandDef>()
    private val subcommandGroups = mutableListOf<SubcommandGroupDef>()
    private val options = mutableListOf<OptionDef<*>>()
    private val buttons = mutableListOf<ButtonDef>()
    private val modals = mutableListOf<ModalDef>()
    private val autocompletes = mutableListOf<AutoCompleteDef>()

    fun guildOnly() { target = CommandTarget.GUILD }
    fun dmOnly() { target = CommandTarget.DM }
    fun allContexts() { target = CommandTarget.ALL }

    fun subcommand(name: String, block: SubcommandBuilder.() -> Unit) {
        subcommands += SubcommandBuilder(name).apply(block).build()
    }

    fun subcommandGroup(name: String, block: SubcommandGroupBuilder.() -> Unit) {
        subcommandGroups += SubcommandGroupBuilder(name).apply(block).build()
    }

    fun <T : Any> option(type: KClass<T>, name: String, block: OptionBuilder<T>.() -> Unit = {}) {
        options += OptionBuilder(name, type).apply(block).build()
    }

    inline fun <reified T : Any> option(name: String, noinline block: OptionBuilder<T>.() -> Unit = {}) =
        option(T::class, name, block)

    fun button(pattern: String) { buttons += ButtonDef(pattern) }
    fun modal(pattern: String) { modals += ModalDef(pattern) }
    fun autocomplete(optionName: String) { autocompletes += AutoCompleteDef(optionName) }

    internal fun build() = CommandDef(
        name = name,
        description = description,
        target = target,
        subcommands = subcommands.toList(),
        subcommandGroups = subcommandGroups.toList(),
        options = options.toList(),
        buttons = buttons.toList(),
        modals = modals.toList(),
        autocompletes = autocompletes.toList()
    )
}

@SlashDsl
class SubcommandGroupBuilder internal constructor(private val name: String) {
    var description: String = ""
    private val subcommands = mutableListOf<SubcommandDef>()

    fun subcommand(name: String, block: SubcommandBuilder.() -> Unit) {
        subcommands += SubcommandBuilder(name).apply(block).build()
    }

    internal fun build() = SubcommandGroupDef(name, description, subcommands.toList())
}

@SlashDsl
class SubcommandBuilder internal constructor(private val name: String) {
    var description: String = ""
    private val options = mutableListOf<OptionDef<*>>()
    private val autocompletes = mutableListOf<AutoCompleteDef>()

    fun <T : Any> option(type: KClass<T>, name: String, block: OptionBuilder<T>.() -> Unit = {}) {
        options += OptionBuilder(name, type).apply(block).build()
    }

    inline fun <reified T : Any> option(name: String, noinline block: OptionBuilder<T>.() -> Unit = {}) =
        option(T::class, name, block)

    fun autocomplete(optionName: String) { autocompletes += AutoCompleteDef(optionName) }

    internal fun build() = SubcommandDef(name, description, options.toList(), autocompletes.toList())
}

@SlashDsl
class OptionBuilder<T : Any> internal constructor(
    private val name: String,
    private val type: KClass<T>
) {
    var description: String = ""
    private var required: Boolean = false
    private val choices = mutableListOf<ChoiceDef<T>>()

    fun required() { required = true }
    fun choice(name: String, value: T) { choices += ChoiceDef(name, value) }

    internal fun build() = OptionDef(name, description, type, required, choices.toList())
}
