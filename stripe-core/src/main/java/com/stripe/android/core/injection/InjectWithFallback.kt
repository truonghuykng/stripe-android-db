package com.stripe.android.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger

/**
 * Try to use an [InjectorKey] to retrieve an [Injector] and inject, if no [Injector] is found,
 * invoke [Injectable.fallbackInitialize] with [fallbackInitializeParam].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <FallbackInitializeParam> Injectable<FallbackInitializeParam, Unit>.injectWithFallback(
    @InjectorKey injectorKey: String?,
    fallbackInitializeParam: FallbackInitializeParam
) {
    val logger = Logger.getInstance(BuildConfig.DEBUG)

    injectorKey?.let {
        WeakMapInjectorRegistry.retrieve(it)
    }?.let {
        logger.info(
            "Injector available, " +
                "injecting dependencies into ${this::class.java.canonicalName}"
        )
        it.inject(this)
    } ?: run {
        logger.info(
            "Injector unavailable, " +
                "initializing dependencies of ${this::class.java.canonicalName}"
        )
        fallbackInitialize(fallbackInitializeParam)
    }
}

/**
 * Try to use an [InjectorKey] to retrieve an [Injector] and inject, if no [Injector] is found,
 * invoke [Injectable.fallbackInitialize] with [fallbackInitializeParam].
 * Used by classes that are responsible for providing a [NonFallbackInjector] to other classes.
 *
 * @return The [NonFallbackInjector] used to inject the dependencies into this class.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <FallbackInitializeParam> Injectable<FallbackInitializeParam, NonFallbackInjector>.injectWithFallback(
    @InjectorKey injectorKey: String?,
    fallbackInitializeParam: FallbackInitializeParam
): NonFallbackInjector {
    val logger = Logger.getInstance(BuildConfig.DEBUG)

    return injectorKey?.let {
        WeakMapInjectorRegistry.retrieve(it)
    }?.let {
        it as? NonFallbackInjector
    }?.let {
        logger.info(
            "Injector available, " +
                "injecting dependencies into ${this::class.java.canonicalName}"
        )
        it.inject(this)
        it
    } ?: run {
        logger.info(
            "Injector unavailable, " +
                "initializing dependencies of ${this::class.java.canonicalName}"
        )
        fallbackInitialize(fallbackInitializeParam)
    }
}
