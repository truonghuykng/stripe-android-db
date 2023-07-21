package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Analytics request sent to q.stripe.com, which is a legacy analytics service used mostly by
 * Payment SDK, analytics are saved in a shared DB table with payment-specific schema.
 *
 * For other SDKs, it is recommended to create a dedicated DB table just for the SDK and write to
 * this table through r.stripe.com. See [AnalyticsRequestV2] for details.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
data class AnalyticsRequest constructor(
    val params: Map<String, String?>,
    override val headers: Map<String, String>
) : StripeRequest() {
    private val query: String = QueryStringFactory.createFromParamsWithEmptyValues(params)

    override val method: Method = Method.GET

    override val mimeType: MimeType = MimeType.Form

    override val retryResponseCodes: Iterable<Int> = HTTP_TOO_MANY_REQUESTS..HTTP_TOO_MANY_REQUESTS

    override val url = listOfNotNull(
        HOST,
        query.takeIf { it.isNotEmpty() }
    ).joinToString("?")

    internal fun toJson(): String? {
        return runCatching { Json.encodeToJsonElement(this).toString() }.getOrNull()
    }

    internal companion object {
        internal const val HOST = "https://q.stripe.com"

        fun fromJson(json: String): AnalyticsRequest? {
            return runCatching {
                Json.decodeFromString<AnalyticsRequest>(json)
            }.getOrNull()
        }
    }
}
