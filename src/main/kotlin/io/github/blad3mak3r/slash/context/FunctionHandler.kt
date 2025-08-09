package io.github.blad3mak3r.slash.context

import kotlin.reflect.KFunction

interface FunctionHandler {

    val function: KFunction<*>

}