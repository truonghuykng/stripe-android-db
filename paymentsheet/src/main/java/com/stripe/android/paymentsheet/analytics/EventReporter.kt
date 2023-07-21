package com.stripe.android.paymentsheet.analytics

import androidx.annotation.Keep
import com.stripe.android.paymentsheet.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface EventReporter {

    /**
     * PaymentSheet has been instantiated or FlowController has started its configuration.
     */
    fun onInit(
        configuration: PaymentSheet.Configuration?,
        isDecoupling: Boolean,
    )

    /**
     * PaymentSheet or FlowController have started loading.
     */
    fun onLoadStarted(
        isDecoupling: Boolean,
    )

    /**
     * PaymentSheet or FlowController have successfully loaded the information required to be
     * rendered.
     */
    fun onLoadSucceeded(
        isDecoupling: Boolean,
        duration: Long?,
    )

    /**
     * PaymentSheet or FlowController have failed to load.
     */
    fun onLoadFailed(
        isDecoupling: Boolean,
        duration: Long?,
        error: Throwable,
    )

    /**
     * PaymentSheet has been dismissed by pressing the close button.
     */
    fun onDismiss(
        isDecoupling: Boolean,
    )

    /**
     * PaymentSheet is displaying the customer's saved payment methods.
     */
    fun onShowExistingPaymentOptions(
        linkEnabled: Boolean,
        currency: String?,
        isDecoupling: Boolean,
    )

    /**
     * PaymentSheet is displaying the form to add a new payment method.
     */
    fun onShowNewPaymentOptionForm(
        linkEnabled: Boolean,
        currency: String?,
        isDecoupling: Boolean,
    )

    /**
     * The customer has selected one of their existing payment methods.
     */
    fun onSelectPaymentOption(
        paymentSelection: PaymentSelection,
        currency: String?,
        isDecoupling: Boolean,
    )

    /**
     * Payment or setup have succeeded.
     */
    fun onPaymentSuccess(
        paymentSelection: PaymentSelection?,
        currency: String?,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
    )

    /**
     * Payment or setup have failed.
     */
    fun onPaymentFailure(
        paymentSelection: PaymentSelection?,
        currency: String?,
        isDecoupling: Boolean,
    )

    /**
     * The client was unable to parse the response from LUXE.
     */
    fun onLpmSpecFailure(
        isDecoupling: Boolean,
    )

    /**
     * The user has auto-filled a text field.
     */
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
