package com.stripe.android.financialconnections.features.partnerauth

import android.webkit.URLUtil
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.stripe.android.core.Logger
import com.stripe.android.financialconnections.analytics.AuthSessionEvent
import com.stripe.android.financialconnections.analytics.AuthSessionEvent.Launched
import com.stripe.android.financialconnections.analytics.AuthSessionEvent.Loaded
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsEvent.PaneLoaded
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.domain.CancelAuthorizationSession
import com.stripe.android.financialconnections.domain.CompleteAuthorizationSession
import com.stripe.android.financialconnections.domain.GetManifest
import com.stripe.android.financialconnections.domain.PollAuthorizationSessionOAuthResults
import com.stripe.android.financialconnections.domain.PostAuthSessionEvent
import com.stripe.android.financialconnections.domain.PostAuthorizationSession
import com.stripe.android.financialconnections.exception.WebAuthFlowFailedException
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.Payload
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenBottomSheet
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenPartnerAuth
import com.stripe.android.financialconnections.features.partnerauth.PartnerAuthState.ViewEffect.OpenUrl
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import com.stripe.android.financialconnections.navigation.NavigationDirections
import com.stripe.android.financialconnections.navigation.NavigationDirections.accountPicker
import com.stripe.android.financialconnections.navigation.NavigationDirections.manualEntry
import com.stripe.android.financialconnections.navigation.NavigationManager
import com.stripe.android.financialconnections.navigation.NavigationState.NavigateToRoute
import com.stripe.android.financialconnections.navigation.toNavigationCommand
import com.stripe.android.financialconnections.presentation.WebAuthFlowState
import com.stripe.android.financialconnections.ui.FinancialConnectionsSheetNativeActivity
import com.stripe.android.financialconnections.utils.UriUtils
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

@Suppress("LongParameterList")
internal class PartnerAuthViewModel @Inject constructor(
    private val completeAuthorizationSession: CompleteAuthorizationSession,
    private val createAuthorizationSession: PostAuthorizationSession,
    private val cancelAuthorizationSession: CancelAuthorizationSession,
    private val eventTracker: FinancialConnectionsAnalyticsTracker,
    @Named(APPLICATION_ID) private val applicationId: String,
    private val uriUtils: UriUtils,
    private val postAuthSessionEvent: PostAuthSessionEvent,
    private val getManifest: GetManifest,
    private val navigationManager: NavigationManager,
    private val pollAuthorizationSessionOAuthResults: PollAuthorizationSessionOAuthResults,
    private val logger: Logger,
    initialState: PartnerAuthState
) : MavericksViewModel<PartnerAuthState>(initialState) {

    init {
        logErrors()
        withState {
            if (it.activeAuthSession == null) {
                launchBrowserIfNonOauth()
                createAuthSession()
            } else {
                logger.debug("Restoring auth session ${it.activeAuthSession}")
                restoreAuthSession()
            }
        }
    }

    private fun restoreAuthSession() {
        suspend {
            // if coming from a process kill, there should be a session
            // re-fetch the manifest and use its active auth session instead of creating a new one
            val manifest: FinancialConnectionsSessionManifest = getManifest()
            val authSession = manifest.activeAuthSession ?: createAuthorizationSession(
                institution = requireNotNull(manifest.activeInstitution),
                allowManualEntry = manifest.allowManualEntry
            )
            Payload(
                authSession = authSession,
                institution = requireNotNull(manifest.activeInstitution),
                isStripeDirect = manifest.isStripeDirect ?: false
            )
        }.execute { copy(payload = it) }
    }

    private fun createAuthSession() {
        suspend {
            val launchedEvent = Launched(Date())
            val manifest: FinancialConnectionsSessionManifest = getManifest()
            val authSession = createAuthorizationSession(
                institution = requireNotNull(manifest.activeInstitution),
                allowManualEntry = manifest.allowManualEntry
            )
            logger.debug("Created auth session ${authSession.id}")
            Payload(
                authSession = authSession,
                institution = requireNotNull(manifest.activeInstitution),
                isStripeDirect = manifest.isStripeDirect ?: false
            ).also {
                // just send loaded event on OAuth flows (prepane). Non-OAuth handled by shim.
                val loadedEvent: Loaded? = Loaded(Date()).takeIf { authSession.isOAuth }
                postAuthSessionEvent(
                    authSession.id,
                    listOfNotNull(launchedEvent, loadedEvent)
                )
            }
        }.execute {
            copy(
                payload = it,
                activeAuthSession = it()?.authSession?.id
            )
        }
    }

    private fun launchBrowserIfNonOauth() {
        onAsync(
            asyncProp = PartnerAuthState::payload,
            onSuccess = {
                // launch auth for non-OAuth (skip pre-pane).
                if (!it.authSession.isOAuth) launchAuthInBrowser()
            }
        )
    }

    private fun logErrors() {
        onAsync(
            PartnerAuthState::payload,
            onFail = {
                logger.error("Error fetching payload / posting AuthSession", it)
                eventTracker.track(FinancialConnectionsEvent.Error(Pane.PARTNER_AUTH, it))
            },
            onSuccess = { eventTracker.track(PaneLoaded(Pane.PARTNER_AUTH)) }
        )
    }

    fun onLaunchAuthClick() {
        viewModelScope.launch {
            awaitState().payload()?.authSession?.let {
                postAuthSessionEvent(it.id, AuthSessionEvent.OAuthLaunched(Date()))
            }
            launchAuthInBrowser()
        }
    }

    private suspend fun launchAuthInBrowser() {
        kotlin.runCatching { requireNotNull(getManifest().activeAuthSession) }
            .onSuccess {
                it.url
                    ?.replaceFirst("stripe-auth://native-redirect/$applicationId/", "")
                    ?.let { setState { copy(viewEffect = OpenPartnerAuth(it)) } }
            }
            .onFailure {
                eventTracker.track(FinancialConnectionsEvent.Error(Pane.PARTNER_AUTH, it))
                logger.error("failed retrieving active session from cache", it)
                setState { copy(authenticationStatus = Fail(it)) }
            }
    }

    fun onSelectAnotherBank() {
        navigationManager.navigate(
            NavigateToRoute(
                command = NavigationDirections.reset,
                popCurrentFromBackStack = true
            )
        )
    }

    fun onWebAuthFlowFinished(
        webStatus: WebAuthFlowState
    ) {
        logger.debug("Web AuthFlow status received $webStatus")
        viewModelScope.launch {
            when (webStatus) {
                WebAuthFlowState.Canceled -> onAuthCancelled()
                is WebAuthFlowState.Failed -> onAuthFailed(webStatus.message, webStatus.reason)
                WebAuthFlowState.InProgress -> setState { copy(authenticationStatus = Loading()) }
                is WebAuthFlowState.Success -> completeAuthorizationSession()
                WebAuthFlowState.Uninitialized -> {}
            }
        }
    }

    private suspend fun onAuthFailed(
        message: String,
        reason: String?
    ) {
        val error = WebAuthFlowFailedException(message, reason)
        kotlin.runCatching {
            logger.debug("Auth failed, cancelling AuthSession")
            val authSession = getManifest().activeAuthSession
            logger.error("Auth failed, cancelling AuthSession", error)
            when {
                authSession != null -> {
                    postAuthSessionEvent(authSession.id, AuthSessionEvent.Failure(Date(), error))
                    cancelAuthorizationSession(authSession.id)
                }

                else -> logger.debug("Could not find AuthSession to cancel.")
            }
            setState { copy(authenticationStatus = Fail(error)) }
        }.onFailure {
            logger.error("failed cancelling session after failed web flow", it)
        }
    }

    private suspend fun onAuthCancelled() {
        kotlin.runCatching {
            logger.debug("Auth cancelled, cancelling AuthSession")
            setState { copy(authenticationStatus = Loading()) }
            val authSession = requireNotNull(getManifest().activeAuthSession)
            val result = cancelAuthorizationSession(authSession.id)
            if (authSession.isOAuth) {
                // For OAuth institutions, create a new session and navigate to its nextPane (prepane).
                logger.debug("Creating a new session for this OAuth institution")
                // Send retry event as we're presenting the prepane again.
                postAuthSessionEvent(authSession.id, AuthSessionEvent.Retry(Date()))
                // for OAuth institutions, we remain on the pre-pane,
                // but create a brand new auth session
                setState { copy(authenticationStatus = Uninitialized) }
                createAuthSession()
            } else {
                // For OAuth institutions, navigate to Session cancellation's next pane.
                postAuthSessionEvent(authSession.id, AuthSessionEvent.Cancel(Date()))
                navigationManager.navigate(
                    NavigateToRoute(
                        command = result.nextPane.toNavigationCommand(),
                        popCurrentFromBackStack = true
                    )
                )
            }
        }.onFailure {
            logger.error("failed cancelling session after cancelled web flow", it)
            setState { copy(authenticationStatus = Fail(it)) }
        }
    }

    private suspend fun completeAuthorizationSession() {
        kotlin.runCatching {
            setState { copy(authenticationStatus = Loading()) }
            val authSession = requireNotNull(getManifest().activeAuthSession)
            postAuthSessionEvent(authSession.id, AuthSessionEvent.Success(Date()))
            if (authSession.isOAuth) {
                logger.debug("Web AuthFlow completed! waiting for oauth results")
                val oAuthResults = pollAuthorizationSessionOAuthResults(authSession)
                logger.debug("OAuth results received! completing session")
                val updatedSession = completeAuthorizationSession(
                    authorizationSessionId = authSession.id,
                    publicToken = oAuthResults.publicToken
                )
                logger.debug("Session authorized!")
                navigationManager.navigate(
                    NavigateToRoute(
                        command = updatedSession.nextPane.toNavigationCommand(),
                        popCurrentFromBackStack = true
                    )
                )
            } else {
                navigationManager.navigate(
                    NavigateToRoute(
                        command = accountPicker,
                        popCurrentFromBackStack = true
                    )
                )
            }
        }.onFailure {
            logger.error("failed authorizing session", it)
            setState { copy(authenticationStatus = Fail(it)) }
        }
    }

    fun onEnterDetailsManuallyClick() = navigationManager.navigate(
        NavigateToRoute(
            command = manualEntry,
            popCurrentFromBackStack = true
        )
    )

    fun onClickableTextClick(uri: String) {
        // if clicked uri contains an eventName query param, track click event.
        viewModelScope.launch {
            uriUtils.getQueryParameter(uri, "eventName")?.let { eventName ->
                eventTracker.track(
                    FinancialConnectionsEvent.Click(
                        eventName,
                        pane = Pane.PARTNER_AUTH
                    )
                )
            }
        }
        if (URLUtil.isNetworkUrl(uri)) {
            setState {
                copy(
                    viewEffect = OpenUrl(
                        uri,
                        Date().time
                    )
                )
            }
        } else {
            val managedUri = PartnerAuthState.ClickableText.values()
                .firstOrNull { uriUtils.compareSchemeAuthorityAndPath(it.value, uri) }
            when (managedUri) {
                PartnerAuthState.ClickableText.DATA -> {
                    setState {
                        copy(
                            viewEffect = OpenBottomSheet(Date().time)
                        )
                    }
                }

                null -> logger.error("Unrecognized clickable text: $uri")
            }
        }
    }

    fun onViewEffectLaunched() {
        setState {
            copy(viewEffect = null)
        }
    }

    companion object : MavericksViewModelFactory<PartnerAuthViewModel, PartnerAuthState> {

        override fun create(
            viewModelContext: ViewModelContext,
            state: PartnerAuthState
        ): PartnerAuthViewModel {
            return viewModelContext.activity<FinancialConnectionsSheetNativeActivity>()
                .viewModel
                .activityRetainedComponent
                .partnerAuthSubcomponent
                .initialState(state)
                .build()
                .viewModel
        }
    }
}
