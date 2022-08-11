package com.stripe.android.financialconnections.networking

import com.stripe.android.financialconnections.financialConnectionsSessionWithNoMoreAccounts
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAcccountsParams
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.model.PartnerAccountsList
import com.stripe.android.financialconnections.moreFinancialConnectionsAccountList
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository

internal class FakeFinancialConnectionsRepository : FinancialConnectionsRepository {

    var getFinancialConnectionsSessionResultProvider: () -> FinancialConnectionsSession =
        { financialConnectionsSessionWithNoMoreAccounts }
    var getAccountsResultProvider: () -> FinancialConnectionsAccountList =
        { moreFinancialConnectionsAccountList }
    var getAuthorizationSessionAccounts: () -> PartnerAccountsList =
        { TODO() }
    var postAuthorizationSessionOAuthResults: () -> MixedOAuthParams =
        { TODO() }

    override suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAcccountsParams: GetFinancialConnectionsAcccountsParams
    ): FinancialConnectionsAccountList = getAccountsResultProvider()

    override suspend fun getFinancialConnectionsSession(
        clientSecret: String
    ): FinancialConnectionsSession = getFinancialConnectionsSessionResultProvider()

    override suspend fun postAuthorizationSessionAccounts(
        clientSecret: String,
        sessionId: String
    ): PartnerAccountsList = getAuthorizationSessionAccounts()

    override suspend fun postAuthorizationSessionOAuthResults(
        clientSecret: String,
        sessionId: String
    ): MixedOAuthParams {
        return postAuthorizationSessionOAuthResults()
    }
}
