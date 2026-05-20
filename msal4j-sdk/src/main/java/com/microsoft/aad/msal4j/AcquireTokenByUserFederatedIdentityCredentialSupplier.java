// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

class AcquireTokenByUserFederatedIdentityCredentialSupplier extends AuthenticationResultSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByUserFederatedIdentityCredentialSupplier.class);
    private UserFederatedIdentityCredentialRequest userFicRequest;

    AcquireTokenByUserFederatedIdentityCredentialSupplier(ConfidentialClientApplication clientApplication,
                                                          UserFederatedIdentityCredentialRequest userFicRequest) {
        super(clientApplication, userFicRequest);
        this.userFicRequest = userFicRequest;
    }

    @Override
    AuthenticationResult execute() throws Exception {
        if (!userFicRequest.parameters.forceRefresh()) {
            LOG.debug("ForceRefresh is false. Attempting cache lookup");
            try {
                // Look up the user's account from the cache by username or OID.
                // User_fic tokens are user-scoped (have homeAccountId), so we must use the
                // account-aware cache lookup path to avoid collisions with app-only tokens.
                IAccount account = findCachedAccount();
                if (account != null) {
                    SilentParameters parameters = SilentParameters
                            .builder(this.userFicRequest.parameters.scopes(), account)
                            .claims(this.userFicRequest.parameters.claims())
                            .tenant(this.userFicRequest.parameters.tenant())
                            .extraHttpHeaders(this.userFicRequest.parameters.extraHttpHeaders())
                            .extraQueryParameters(this.userFicRequest.parameters.extraQueryParameters())
                            .build();

                    RequestContext context = new RequestContext(
                            this.clientApplication,
                            PublicApi.ACQUIRE_TOKEN_SILENTLY,
                            parameters);

                    SilentRequest silentRequest = new SilentRequest(
                            parameters,
                            this.clientApplication,
                            context,
                            null);

                    AcquireTokenSilentSupplier supplier = new AcquireTokenSilentSupplier(
                            this.clientApplication,
                            silentRequest);

                    return supplier.execute();
                } else {
                    LOG.debug("No cached account found for user. Going to IdP.");
                }
            } catch (MsalClientException ex) {
                LOG.debug("Cache lookup failed: {}", ex.getMessage());
            }
        } else {
            LOG.debug("ForceRefresh is true. Skipping cache lookup");
        }

        return acquireTokenByUserFic();
    }

    /**
     * Finds a cached account matching the user_fic request's username or userObjectId.
     * Returns null if no matching account is in the cache (first call scenario).
     */
    private IAccount findCachedAccount() {
        try {
            // Use synchronous cache read to avoid potential deadlock when running on a
            // single-thread executor (getAccounts() submits work to the same executor).
            Set<IAccount> accounts = this.clientApplication.tokenCache().getAccounts(
                    this.clientApplication.clientId());
            if (accounts == null || accounts.isEmpty()) {
                return null;
            }

            String username = userFicRequest.parameters.username();
            java.util.UUID userObjectId = userFicRequest.parameters.userObjectId();

            for (IAccount account : accounts) {
                // Match by OID — homeAccountId format is "oid.tid", case-insensitive
                if (userObjectId != null && account.homeAccountId() != null) {
                    String[] parts = account.homeAccountId().split("\\.");
                    if (parts.length > 0 && userObjectId.toString().equalsIgnoreCase(parts[0])) {
                        return account;
                    }
                }
                // Match by username (UPN) — case-insensitive
                if (username != null && !username.isEmpty()
                        && account.username() != null
                        && username.equalsIgnoreCase(account.username())) {
                    return account;
                }
            }
        } catch (Exception ex) {
            LOG.debug("Error looking up cached accounts: {}", ex.getMessage());
        }
        return null;
    }

    private AuthenticationResult acquireTokenByUserFic() throws Exception {
        AcquireTokenByAuthorizationGrantSupplier supplier = new AcquireTokenByAuthorizationGrantSupplier(
                this.clientApplication,
                userFicRequest,
                null);

        return supplier.execute();
    }
}
