package tv.blademaker.slash.internal.handlers

import tv.blademaker.slash.BaseSlashCommand
import kotlin.reflect.KFunction

interface Handler {

    val path: String
    val parent: BaseSlashCommand
    val function: KFunction<*>

}