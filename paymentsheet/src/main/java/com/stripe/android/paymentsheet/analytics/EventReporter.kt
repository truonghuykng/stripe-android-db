package com.stripe.android.paymentsheet.analytics

import androidx.annotation.Keep
import com.stripe.android.paymentsheet.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface EventReporter {

    fun onInit(
        configuration: PaymentSheet.Configuration?,
        isDecoupling: Boolean,
    )

    fun onDismiss(
        isDecoupling: Boolean,
    )

    fun onShowExistingPaymentOptions(
        linkEnabled: Boolean,
        currency: String?,
        isDecoupling: Boolean,
    )

    fun onShowNewPaymentOptionForm(
        linkEnabled: Boolean,
        currency: String?,
        isDecoupling: Boolean,
    )

    fun onSelectPaymentOption(
        paymentSelection: PaymentSelection,
        currency: String?,
        isDecoupling: Boolean,
    )

    fun onPaymentSuccess(
        paymentSelection: PaymentSelection?,
        currency: String?,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
    )

    fun onPaymentFailure(
        paymentSelection: PaymentSelection?,
        currency: String?,
        isDecoupling: Boolean,
    )

    fun onLpmSpecFailure(
        isDecoupling: Boolean,
    )

    fun onAutofill(
        type: String,
        isDecoupling: Boolean,
    )

    enum class Mode(val code: String) {
        Complete("complete"),
        Custom("custom");

        @Keep
        override fun toString(): String = code
    }
}
