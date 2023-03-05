package tv.blademaker.slash.internal.handlers

import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.annotations.OnUserContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation

class UserContextHandler(override val parent: BaseSlashCommand, override val function: KFunction<*>) : Handler {

    private val annotation = function.findAnnotation<OnUserContext>()!!

    override val path = buildString {
        append(parent.commandName)
        if (annotation.group.isNotBlank()) append("/${annotation.group}")
        if (annotation.name.isNotBlank()) append("/${annotation.name}")
    }


}