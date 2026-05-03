package io.github.blad3mak3r.slash.dsl

import kotlin.reflect.KClass

enum class CommandTarget { GUILD, DM, ALL }

data class CommandDef(
    val name: String,
    val description: String,
    val target: CommandTarget,
    val subcommands: List<SubcommandDef>,
    val subcommandGroups: List<SubcommandGroupDef>,
    val options: List<OptionDef<*>>,
    val buttons: List<ButtonDef>,
    val modals: List<ModalDef>,
    val autocompletes: List<AutoCompleteDef>
)

data class SubcommandGroupDef(
    val name: String,
    val description: String,
    val subcommands: List<SubcommandDef>
)

data class SubcommandDef(
    val name: String,
    val description: String,
    val options: List<OptionDef<*>>,
    val autocompletes: List<AutoCompleteDef>
)

data class OptionDef<T : Any>(
    val name: String,
    val description: String,
    val type: KClass<T>,
    val required: Boolean,
    val choices: List<ChoiceDef<T>>
)

data class ChoiceDef<T : Any>(val name: String, val value: T)

data class ButtonDef(val pattern: String)

data class ModalDef(val pattern: String)

data class AutoCompleteDef(val optionName: String)
