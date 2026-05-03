package io.github.blad3mak3r.slash.annotations

import kotlin.reflect.KClass

/**
 * Declares one or more [SlashPrecondition] classes that must all pass before
 * the annotated command handler is executed.
 *
 * Can be applied at both class and function level; both sets are evaluated.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Require(vararg val value: KClass<out SlashPrecondition>)
