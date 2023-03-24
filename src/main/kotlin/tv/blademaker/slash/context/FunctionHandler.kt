package tv.blademaker.slash.context

import kotlin.reflect.KFunction

interface FunctionHandler {

    val function: KFunction<*>

}