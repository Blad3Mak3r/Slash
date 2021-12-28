package tv.blademaker.slash.internal

import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.context.InteractionContext
import kotlin.reflect.KFunction
import kotlin.reflect.KType

internal class FunctionParameter(
    private val command: BaseSlashCommand,
    private val function: KFunction<*>,
    val name: String,
    private val type: KType
) {
    private val isOptional = type.isMarkedNullable

    fun compile(ctx: InteractionContext): Any? {
        val eventOption = ctx.getOptionOrNull(name)

        if (!isOptional && eventOption == null) error("Parameter marked as non-optional is not present in application event: $this")

        val converter = ValidOptionTypes.get(type)
            ?: error("Not found valid OptionCompiler for ${type.classifier}")

        return converter.convert(eventOption)
    }

    override fun toString(): String {
        return "${command.commandName}#${function.name}(ctx, ...$name)"
    }
}