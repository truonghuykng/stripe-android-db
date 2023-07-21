package com.stripe.android.core.networking

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DefaultAnalyticsRequestExecutor private constructor(
    private val context: Context,
    private val workContext: CoroutineContext,
    private val shouldEnqueue: Boolean,
    private val enableLogging: Boolean,
) : AnalyticsRequestExecutor {

    private val stripeNetworkClient: StripeNetworkClient by lazy {
        DefaultStripeNetworkClient(
            workContext = workContext,
            logger = Logger.getInstance(enableLogging),
        )
    }

    @Inject
    constructor(
        context: Context,
    ) : this(
        context = context,
        workContext = Dispatchers.IO,
        shouldEnqueue = true,
        enableLogging = BuildConfig.DEBUG,
    )

    constructor(
        context: Context,
        enableLogging: Boolean = BuildConfig.DEBUG,
        workContext: CoroutineContext = Dispatchers.IO,
    ) : this(
        context = context,
        workContext = workContext,
        shouldEnqueue = true,
        enableLogging = enableLogging,
    )

    constructor(
        context: Context,
        logger: Logger,
        @IOContext workContext: CoroutineContext,
        shouldEnqueue: Boolean = true,
    ) : this(
        context = context.applicationContext,
        workContext = workContext,
        enableLogging = logger != Logger.noop(),
        shouldEnqueue = shouldEnqueue,
    )

    /**
     * Make the request and ignore the response.
     */
    override fun executeAsync(request: AnalyticsRequest) {
        if (shouldEnqueue) {
            enqueue(request)
        } else {
            execute(request)
        }
    }

    private fun execute(request: AnalyticsRequest) {
        val logger = Logger.getInstance(enableLogging)
        logger.info("Event: ${request.params[FIELD_EVENT]}")

        CoroutineScope(workContext).launch {
            runCatching {
                stripeNetworkClient.executeRequest(request)
            }.onFailure {
                logger.error("Exception while making analytics request", it)
            }
        }
    }

    private val workManager: WorkManager by lazy {
        WorkManager.getInstance(context)
    }

    class Worker(
        appContext: Context,
        params: WorkerParameters,
    ) : CoroutineWorker(appContext, params) {

        override suspend fun doWork(): Result {
            val data = inputData.getString(FIELD_DATA) ?: return Result.failure()
            val request = AnalyticsRequest.fromJson(data) ?: return Result.failure()
            val enableLogging = inputData.getBoolean(FIELD_ENABLE_LOGGING, false)

            val logger = Logger.getInstance(enableLogging)
            val stripeNetworkClient = DefaultStripeNetworkClient(logger = logger)

            logger.info("Event: ${request.params[FIELD_EVENT]}")

            return runCatching {
                stripeNetworkClient.executeRequest(request)
            }.onFailure {
                logger.error("Exception while making analytics request", it)
            }.fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
        }
    }

    /**
     * Make the request and ignore the response.
     */
    private fun enqueue(request: AnalyticsRequest) {
        val inputData = Data.Builder()
            .putString(FIELD_DATA, request.toJson())
            .putBoolean(FIELD_ENABLE_LOGGING, false)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<Worker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        workManager.enqueue(workRequest)
    }

    internal companion object {
        const val FIELD_DATA = "data"
        const val FIELD_ENABLE_LOGGING = "enable_logging"
        const val FIELD_EVENT = "event"
    }
}
