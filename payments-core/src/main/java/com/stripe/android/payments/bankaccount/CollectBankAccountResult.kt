package com.stripe.android.payments.bankaccount

import android.os.Parcelable
import com.stripe.android.model.PaymentIntent
import kotlinx.parcelize.Parcelize

/**
 * The result of an attempt to collect a bank account
 */
internal sealed class CollectBankAccountResult : Parcelable {

    // TODO manage setup and payment intents.
    @Parcelize
    data class Completed(
        val paymentIntent: PaymentIntent
    ) : CollectBankAccountResult()

    @Parcelize
    data class Failed(
        val error: Throwable
    ) : CollectBankAccountResult()
}