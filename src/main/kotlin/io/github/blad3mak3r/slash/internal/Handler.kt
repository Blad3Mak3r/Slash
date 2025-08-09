package io.github.blad3mak3r.slash.internal

import io.github.blad3mak3r.slash.BaseSlashCommand
import kotlin.reflect.KFunction

interface Handler {

    val path: String
    val parent: BaseSlashCommand
    val function: KFunction<*>

}