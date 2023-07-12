package com.stripe.android.financialconnections.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.NavType.EnumType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.model.LinkAccountSessionPaymentAccount.MicrodepositVerificationMethod

internal interface NavigationCommand {
    val baseRoute: String
    val arguments: List<NamedNavArgument>

    val destination: String
        get() = "$baseRoute?" + arguments.joinToString(separator = ",") { (key, value) ->
            "$key={$value}"
        }

    fun composable(
        navGraphBuilder: NavGraphBuilder,
        content: @Composable (NavBackStackEntry) -> Unit
    ) = navGraphBuilder.composable(
        route = destination,
        arguments = arguments,
        content = content
    )

    operator fun invoke(
        referrer: Pane?,
        params: Map<String, String?> = emptyMap()
    ): String
}

internal object NavigationDirections {

    private fun commonArguments() = listOf(
        navArgument("referrer_pane") {
            type = NavType.StringType
            nullable = true
            defaultValue = null
        }
    )

    private fun String.addParameters(
        referrer: Pane?,
        params: Map<String, String?>
    ): String {
        val commonParams = listOf("referrer_pane" to referrer?.value)
        return StringBuilder(this).apply {
            append('?')
            (commonParams + params.toList())
                .joinTo(this, separator = ",") { (key, value) -> "$key=$value" }
        }.toString()
    }

    val institutionPicker = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "bank_picker"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val consent = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "bank_intro"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val partnerAuth = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "partner_auth"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val accountPicker = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "account_picker"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val success = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "success"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val manualEntry = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "manual_entry"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val attachLinkedPaymentAccount = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "attach_linked_payment_account"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val networkingLinkSignup = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "networking_link_signup_pane"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val networkingLinkLoginWarmup = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "networking_link_login_warmup"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val networkingLinkVerification = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "networking_link_verification_pane"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val networkingSaveToLinkVerification = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "networking_save_to_link_verification_pane"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val linkAccountPicker = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "linkaccount_picker"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val linkStepUpVerification = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "link_step_up_verification"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    val reset = object : NavigationCommand {
        override val arguments = commonArguments()
        override val baseRoute: String = "reset"
        override fun invoke(
            referrer: Pane?,
            params: Map<String, String?>
        ): String = baseRoute.addParameters(referrer, params)
    }

    object ManualEntrySuccess : NavigationCommand {

        private const val KEY_MICRODEPOSITS = "microdeposits"
        private const val KEY_LAST4 = "last4"

        override val baseRoute: String = "manual_entry_success"

        override val arguments = commonArguments() + listOf(
            navArgument(KEY_LAST4) { type = NavType.StringType },
            navArgument(KEY_MICRODEPOSITS) {
                type = EnumType(MicrodepositVerificationMethod::class.java)
            }
        )

        override fun invoke(referrer: Pane?, params: Map<String, String?>): String {
            return baseRoute.addParameters(referrer, params)
        }

        fun argMap(
            microdepositVerificationMethod: MicrodepositVerificationMethod,
            last4: String?
        ): Map<String, String?> = mapOf(
            KEY_MICRODEPOSITS to microdepositVerificationMethod.value,
            KEY_LAST4 to last4
        )

        fun microdeposits(backStackEntry: NavBackStackEntry): MicrodepositVerificationMethod =
            backStackEntry.arguments?.getString(KEY_MICRODEPOSITS)?.let { enumValue ->
                MicrodepositVerificationMethod.values().first { it.value == enumValue }
            } ?: MicrodepositVerificationMethod.UNKNOWN

        fun last4(backStackEntry: NavBackStackEntry): String? =
            backStackEntry.arguments?.getString(KEY_LAST4)
    }
}
