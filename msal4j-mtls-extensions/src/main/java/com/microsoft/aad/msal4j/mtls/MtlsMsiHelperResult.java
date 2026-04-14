// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

/**
 * Result returned by {@link MtlsMsiClient} after a successful mTLS PoP token acquisition.
 */
public class MtlsMsiHelperResult {

    private final String accessToken;
    private final String tokenType;
    private final int expiresIn;
    private final String bindingCertificate;
    private final String tenantId;
    private final String clientId;

    public MtlsMsiHelperResult(
            String accessToken,
            String tokenType,
            int expiresIn,
            String bindingCertificate,
            String tenantId,
            String clientId) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.bindingCertificate = bindingCertificate;
        this.tenantId = tenantId;
        this.clientId = clientId;
    }

    /** The mTLS PoP access token. */
    public String getAccessToken() { return accessToken; }

    /** Always {@code "mtls_pop"}. */
    public String getTokenType() { return tokenType; }

    /** Seconds until expiry, as of the moment the subprocess returned. */
    public int getExpiresIn() { return expiresIn; }

    /**
     * PEM-encoded binding certificate (the KeyGuard-backed certificate whose
     * thumbprint is bound into the token's {@code cnf.x5t#S256} claim).
     */
    public String getBindingCertificate() { return bindingCertificate; }

    /** Tenant ID from the issued token. May be null. */
    public String getTenantId() { return tenantId; }

    /** Object ID / client ID from the issued token. May be null. */
    public String getClientId() { return clientId; }
}
