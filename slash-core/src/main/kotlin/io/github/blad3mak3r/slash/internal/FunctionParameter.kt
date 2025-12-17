package io.github.blad3mak3r.slash.internal

import io.github.blad3mak3r.slash.BaseSlashCommand
import io.github.blad3mak3r.slash.context.AutoCompleteContext
import io.github.blad3mak3r.slash.context.SlashCommandContext
import kotlin.reflect.KFunction
import kotlin.reflect.KType

internal class FunctionParameter(
    private val command: BaseSlashCommand,
    private val function: KFunction<*>,
    val name: String,
    private val type: KType
) {
    private val isOptional = type.isMarkedNullable

    fun compile(ctx: SlashCommandContext): Any? {
        val eventOption = ctx.getOption(name)

        if (!isOptional && eventOption == null) error("Parameter marked as non-optional is not present in slash command event: $this")

        val converter = ValidOptionTypes.get(type)
            ?: error("Not found valid OptionCompiler for ${type.classifier}")

        return converter.convert(eventOption)
    }

    fun compile(ctx: AutoCompleteContext): Any? {
        val eventOption = ctx.getOption(name)

        if (!isOptional && eventOption == null) error("Parameter marked as non-optional is not present in auto-complete event: $this")

        val converter = ValidOptionTypes.get(type)
            ?: error("Not found valid OptionCompiler for ${type.classifier}")

        return converter.convert(eventOption)
    }

    override fun toString(): String {
        return "${command.commandName}#${function.name}(ctx, ...$name)"
    }
}