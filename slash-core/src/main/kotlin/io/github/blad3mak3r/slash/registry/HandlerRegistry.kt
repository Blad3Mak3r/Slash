package io.github.blad3mak3r.slash.registry

import io.github.blad3mak3r.slash.annotations.InteractionTarget
import io.github.blad3mak3r.slash.context.*
import java.util.regex.Pattern

/**
 * Central registry populated by [CommandRegistrar] implementations at startup.
 *
 * Slash and context-command lookups are O(1) via [HashMap].
 * Button and modal lookups are O(n) with pre-compiled [Pattern]s.
 */
class HandlerRegistry {

    // Slash commands — key = command path (e.g. "greet", "admin/ban/user")
    internal val slash = HashMap<String, SlashEntry>()

    // Auto-complete handlers — key = "path:optionName"
    internal val autoComplete = HashMap<String, AutoCompleteEntry>()

    // Pattern-matched handlers
    internal val buttons = mutableListOf<ButtonEntry>()
    internal val modals  = mutableListOf<ModalEntry>()

    // Context-menu commands — key = lowercased command name
    internal val message = HashMap<String, MessageEntry>()
    internal val user    = HashMap<String, UserEntry>()

    // ── Registration ─────────────────────────────────────────────────────────

    fun registerSlash(
        path: String,
        target: InteractionTarget = InteractionTarget.ALL,
        supportDetached: Boolean = false,
        permissions: PermissionsConfig? = null,
        rateLimit: RateLimitConfig? = null,
        preconditions: Array<out Precondition> = emptyArray(),
        handler: suspend (SlashCommandContext) -> Unit
    ) {
        check(!slash.containsKey(path)) { "Slash handler for path '$path' is already registered." }
        slash[path] = SlashEntry(path, target, supportDetached, permissions, rateLimit, preconditions, handler)
    }

    fun registerAutoComplete(
        path: String,
        optionName: String,
        handler: suspend (AutoCompleteContext) -> Unit
    ) {
        val key = "$path:$optionName"
        check(!autoComplete.containsKey(key)) {
            "AutoComplete handler for path '$path' / option '$optionName' is already registered."
        }
        autoComplete[key] = AutoCompleteEntry(path, optionName, handler)
    }

    fun registerButton(
        pattern: String,
        handler: suspend (ButtonContext) -> Unit
    ) {
        buttons += ButtonEntry(Pattern.compile(pattern), handler)
    }

    fun registerModal(
        pattern: String,
        handler: suspend (ModalContext) -> Unit
    ) {
        modals += ModalEntry(Pattern.compile(pattern), handler)
    }

    fun registerUserCommand(
        name: String,
        preconditions: Array<out Precondition> = emptyArray(),
        handler: suspend (UserCommandContext) -> Unit
    ) {
        val key = name.lowercase()
        check(!user.containsKey(key)) { "User command '$name' is already registered." }
        user[key] = UserEntry(name, preconditions, handler)
    }

    fun registerMessageCommand(
        name: String,
        preconditions: Array<out Precondition> = emptyArray(),
        handler: suspend (MessageCommandContext) -> Unit
    ) {
        val key = name.lowercase()
        check(!message.containsKey(key)) { "Message command '$name' is already registered." }
        message[key] = MessageEntry(name, preconditions, handler)
    }

    /** Returns a human-readable summary of everything registered so far. */
    fun summary(): String =
        "slash=${slash.size}, autoComplete=${autoComplete.size}, " +
        "buttons=${buttons.size}, modals=${modals.size}, " +
        "message=${message.size}, user=${user.size}"

    fun slashCount(): Int = slash.size
    fun autoCompleteCount(): Int = autoComplete.size
    fun buttonCount(): Int = buttons.size
    fun modalCount(): Int = modals.size
    fun messageCount(): Int = message.size
    fun userCount(): Int = user.size
    fun slashPaths(): Set<String> = slash.keys.toSortedSet()
}

// ── Entry types ───────────────────────────────────────────────────────────────

internal data class SlashEntry(
    val path: String,
    val target: InteractionTarget,
    val supportDetached: Boolean,
    val permissions: PermissionsConfig?,
    val rateLimit: RateLimitConfig?,
    val preconditions: Array<out Precondition>,
    val handler: suspend (SlashCommandContext) -> Unit
)

internal data class AutoCompleteEntry(
    val path: String,
    val optionName: String,
    val handler: suspend (AutoCompleteContext) -> Unit
)

internal class ButtonEntry(
    val pattern: Pattern,
    val handler: suspend (ButtonContext) -> Unit
) {
    fun matches(id: String) = pattern.matcher(id).matches()
    fun matcher(id: String): java.util.regex.Matcher = pattern.matcher(id)
}

internal class ModalEntry(
    val pattern: Pattern,
    val handler: suspend (ModalContext) -> Unit
) {
    fun matches(id: String) = pattern.matcher(id).matches()
    fun matcher(id: String): java.util.regex.Matcher = pattern.matcher(id)
}

internal data class UserEntry(
    val name: String,
    val preconditions: Array<out Precondition>,
    val handler: suspend (UserCommandContext) -> Unit
)

internal data class MessageEntry(
    val name: String,
    val preconditions: Array<out Precondition>,
    val handler: suspend (MessageCommandContext) -> Unit
)
