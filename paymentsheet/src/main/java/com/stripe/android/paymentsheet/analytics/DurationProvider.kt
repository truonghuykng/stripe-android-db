package com.stripe.android.paymentsheet.analytics

import android.os.SystemClock
import javax.inject.Inject

internal interface DurationProvider {
    fun start(key: String)
    fun end(key: String): Long?
}

internal class DefaultDurationProvider @Inject constructor() : DurationProvider {

    private val store = mutableMapOf<String, Long>()

    override fun start(key: String) {
        val startTime = SystemClock.elapsedRealtime()
        store[key] = startTime
    }

    override fun end(key: String): Long? {
        val startTime = store.remove(key) ?: return null
        return SystemClock.elapsedRealtime() - startTime
    }
}
