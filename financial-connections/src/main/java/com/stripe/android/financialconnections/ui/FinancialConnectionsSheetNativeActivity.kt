package com.stripe.android.financialconnections.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.compose.collectAsState
import com.airbnb.mvrx.withState
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.features.accountpicker.AccountPickerScreen
import com.stripe.android.financialconnections.features.attachpayment.AttachPaymentScreen
import com.stripe.android.financialconnections.features.common.CloseDialog
import com.stripe.android.financialconnections.features.consent.ConsentScreen
import com.stripe.android.financialconnections.features.institutionpicker.InstitutionPickerScreen
import com.stripe.android.financialconnections.features.linkaccountpicker.LinkAccountPickerScreen
import com.stripe.android.financialconnections.features.linkstepupverification.LinkStepUpVerificationScreen
import com.stripe.android.financialconnections.features.manualentry.ManualEntryScreen
import com.stripe.android.financialconnections.features.manualentrysuccess.ManualEntrySuccessScreen
import com.stripe.android.financialconnections.features.networkinglinkloginwarmup.NetworkingLinkLoginWarmupScreen
import com.stripe.android.financialconnections.features.networkinglinksignup.NetworkingLinkSignupScreen
import com.stripe.android.financialconnections.features.networkinglinkverification.NetworkingLinkVerificationScreen
import com.stripe.android.financialconnections.features.networkingsavetolinkverification.NetworkingSaveToLinkVerificationScreen
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthScreen
import com.stripe.android.financialconnections.features.reset.ResetScreen
import com.stripe.android.financialconnections.features.success.SuccessScreen
import com.stripe.android.financialconnections.launcher.FinancialConnectionsSheetNativeActivityArgs
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.NavigationState
import com.stripe.android.financialconnections.navigation.toRoute
import com.stripe.android.financialconnections.presentation.CreateBrowserIntentForUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.Finish
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewEffect.OpenUrl
import com.stripe.android.financialconnections.presentation.FinancialConnectionsSheetNativeViewModel
import com.stripe.android.financialconnections.ui.theme.FinancialConnectionsTheme
import com.stripe.android.financialconnections.utils.argsOrNull
import com.stripe.android.financialconnections.utils.viewModelLazy
import com.stripe.android.uicore.image.StripeImageLoader
import javax.inject.Inject

internal class FinancialConnectionsSheetNativeActivity : AppCompatActivity(), MavericksView {

    val args by argsOrNull<FinancialConnectionsSheetNativeActivityArgs>()

    val viewModel: FinancialConnectionsSheetNativeViewModel by viewModelLazy()

    @Inject
    lateinit var navigationManager: NavigationManager

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var imageLoader: StripeImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args == null) {
            finish()
        } else {
            viewModel.activityRetainedComponent.inject(this)
            viewModel.onEach { postInvalidate() }
            onBackPressedDispatcher.addCallback { viewModel.onBackPressed() }
            setContent {
                FinancialConnectionsTheme {
                    Column {
                        Box(modifier = Modifier.weight(1f)) {
                            val closeDialog = viewModel.collectAsState { it.closeDialog }
                            val firstPane =
                                viewModel.collectAsState { it.initialPane }
                            val reducedBranding =
                                viewModel.collectAsState { it.reducedBranding }
                            closeDialog.value?.let {
                                CloseDialog(
                                    description = it.description,
                                    onConfirmClick = viewModel::onCloseConfirm,
                                    onDismissClick = viewModel::onCloseDismiss
                                )
                            }
                            NavHost(
                                firstPane.value,
                                reducedBranding.value
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * handle state changes here.
     */
    override fun invalidate() {
        withState(viewModel) { state ->
            state.viewEffect?.let { viewEffect ->
                when (viewEffect) {
                    is OpenUrl -> startActivity(
                        CreateBrowserIntentForUrl(
                            context = this,
                            uri = Uri.parse(viewEffect.url)
                        )
                    )

                    is Finish -> {
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(EXTRA_RESULT, viewEffect.result)
                        )
                        finish()
                    }
                }
                viewModel.onViewEffectLaunched()
            }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Suppress("LongMethod")
    @Composable
    fun NavHost(
        initialPane: Pane,
        reducedBranding: Boolean
    ) {
        val context = LocalContext.current
        val navController = rememberNavController()
        val uriHandler = remember { CustomTabUriHandler(context) }
        val initialDestination = remember(initialPane) { initialPane.toRoute().destination }
        NavigationEffect(navController)
        CompositionLocalProvider(
            LocalReducedBranding provides reducedBranding,
            LocalNavHostController provides navController,
            LocalImageLoader provides imageLoader,
            LocalUriHandler provides uriHandler
        ) {
            NavHost(navController, startDestination = initialDestination) {
                NavigationDirections.consent.composable(this) {
                    LaunchedPane(Pane.CONSENT, it)
                    BackHandler(navController, Pane.CONSENT)
                    ConsentScreen()
                }
                NavigationDirections.manualEntry.composable(this) {
                    LaunchedPane(Pane.MANUAL_ENTRY, it)
                    BackHandler(navController, Pane.MANUAL_ENTRY)
                    ManualEntryScreen()
                }
                NavigationDirections.ManualEntrySuccess.composable(this) {
                    LaunchedPane(Pane.MANUAL_ENTRY_SUCCESS, it)
                    BackHandler(navController, Pane.MANUAL_ENTRY_SUCCESS)
                    ManualEntrySuccessScreen(it)
                }
                NavigationDirections.institutionPicker.composable(this) {
                    LaunchedPane(Pane.INSTITUTION_PICKER, it)
                    BackHandler(navController, Pane.INSTITUTION_PICKER)
                    InstitutionPickerScreen()
                }
                NavigationDirections.partnerAuth.composable(this) {
                    LaunchedPane(Pane.PARTNER_AUTH, it)
                    BackHandler(navController, Pane.PARTNER_AUTH)
                    PartnerAuthScreen()
                }
                NavigationDirections.accountPicker.composable(this) {
                    LaunchedPane(Pane.ACCOUNT_PICKER, it)
                    BackHandler(navController, Pane.ACCOUNT_PICKER)
                    AccountPickerScreen()
                }
                NavigationDirections.success.composable(this) {
                    LaunchedPane(Pane.SUCCESS, it)
                    BackHandler(navController, Pane.SUCCESS)
                    SuccessScreen()
                }
                NavigationDirections.reset.composable(this) {
                    LaunchedPane(Pane.RESET, it)
                    BackHandler(navController, Pane.RESET)
                    ResetScreen()
                }
                NavigationDirections.attachLinkedPaymentAccount.composable(this) {
                    LaunchedPane(Pane.ATTACH_LINKED_PAYMENT_ACCOUNT, it)
                    BackHandler(navController, Pane.ATTACH_LINKED_PAYMENT_ACCOUNT)
                    AttachPaymentScreen()
                }
                NavigationDirections.networkingLinkSignup.composable(this) {
                    LaunchedPane(Pane.NETWORKING_LINK_SIGNUP_PANE, it)
                    BackHandler(navController, Pane.NETWORKING_LINK_SIGNUP_PANE)
                    NetworkingLinkSignupScreen()
                }
                NavigationDirections.networkingLinkLoginWarmup.composable(this) {
                    LaunchedPane(Pane.NETWORKING_LINK_LOGIN_WARMUP, it)
                    BackHandler(navController, Pane.NETWORKING_LINK_LOGIN_WARMUP)
                    NetworkingLinkLoginWarmupScreen()
                }
                NavigationDirections.networkingLinkVerification.composable(this) {
                    LaunchedPane(Pane.NETWORKING_LINK_VERIFICATION, it)
                    BackHandler(navController, Pane.NETWORKING_LINK_VERIFICATION)
                    NetworkingLinkVerificationScreen()
                }
                NavigationDirections.networkingSaveToLinkVerification.composable(this) {
                    LaunchedPane(Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION, it)
                    BackHandler(navController, Pane.NETWORKING_SAVE_TO_LINK_VERIFICATION)
                    NetworkingSaveToLinkVerificationScreen()
                }
                NavigationDirections.linkAccountPicker.composable(this) {
                    LaunchedPane(Pane.LINK_ACCOUNT_PICKER, it)
                    BackHandler(navController, Pane.LINK_ACCOUNT_PICKER)
                    LinkAccountPickerScreen()
                }
                NavigationDirections.linkStepUpVerification.composable(this) {
                    LaunchedPane(Pane.LINK_STEP_UP_VERIFICATION, it)
                    BackHandler(navController, Pane.LINK_STEP_UP_VERIFICATION)
                    LinkStepUpVerificationScreen()
                }
            }
        }
    }

    @Composable
    private fun BackHandler(navController: NavHostController, pane: Pane) {
        androidx.activity.compose.BackHandler(true) {
            viewModel.onBackClick(pane)
            if (navController.popBackStack().not()) onBackPressedDispatcher.onBackPressed()
        }
    }

    @Composable
    private fun LaunchedPane(
        pane: Pane,
        navBackStackEntry: NavBackStackEntry
    ) {
        LaunchedEffect(Unit) {
            viewModel.onPaneLaunched(
                referrer = navBackStackEntry.arguments
                    ?.getString("referrer_pane")
                    ?.let { Pane.fromValue(it) },
                pane = pane
            )
        }
    }

    /**
     * Handles new intents in the form of the redirect from the custom tab hosted auth flow
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        viewModel.handleOnNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    @Composable
    private fun NavigationEffect(
        navController: NavHostController
    ) {
        val navigationState by navigationManager.navigationState.collectAsState()

        LaunchedEffect(navigationState) {
            logger.debug("updateNavigationState to $navigationState")
            val from = navController.currentDestination?.route
            when (val viewState = navigationState) {
                is NavigationState.NavigateToRoute -> {
                    navigateToRoute(viewState, from, navController)
                    navigationManager.onNavigated(navigationState)
                }

                is NavigationState.Idle -> {}
            }
        }
    }

    private fun navigateToRoute(
        viewState: NavigationState.NavigateToRoute,
        from: String?,
        navController: NavHostController
    ) {
        val destination = viewState.command(
            referrer = viewState.referrer,
            params = viewState.params
        )
        if (destination.isNotEmpty() && destination != from) {
            logger.debug("Navigating from $from to $destination")
            navController.navigate(destination) {
                launchSingleTop = true
                val currentScreen: String? = navController.currentBackStackEntry?.destination?.route
                if (currentScreen != null && viewState.popCurrentFromBackStack) {
                    popUpTo(currentScreen) { inclusive = true }
                }
            }
        }
    }

    internal companion object {
        internal const val EXTRA_RESULT = "result"
    }
}

internal val LocalNavHostController = staticCompositionLocalOf<NavHostController> {
    error("No NavHostController provided")
}

internal val LocalReducedBranding = staticCompositionLocalOf<Boolean> {
    error("No ReducedBranding provided")
}

internal val LocalImageLoader = staticCompositionLocalOf<StripeImageLoader> {
    error("No ImageLoader provided")
}
