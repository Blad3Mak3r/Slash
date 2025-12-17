package io.github.blad3mak3r.slash.internal

import io.github.blad3mak3r.slash.context.InteractionContext

abstract class InterceptorHandler<C : InteractionContext<*>, T : Interceptor<C>> {

    private val interceptors: MutableList<T> = mutableListOf()

    internal suspend fun runInterceptors(ctx: C): Boolean {
        if (interceptors.isEmpty()) return true
        return interceptors.all { it.intercept(ctx) }
    }


    fun addInterceptor(interceptor: T) {
        if (interceptors.contains(interceptor)) error("Check already registered.")
        interceptors.add(interceptor)
    }

}