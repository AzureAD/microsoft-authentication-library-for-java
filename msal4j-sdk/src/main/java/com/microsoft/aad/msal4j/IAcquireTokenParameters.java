// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

/**
 * Interface representing common parameters shared across all token acquisition methods in MSAL.
 * <p>
 * This interface defines the core parameters that are common to all token acquisition operations,
 * regardless of the specific authentication flow being used (interactive, silent, username/password, etc.).
 * Various parameter classes in the library implement this interface to provide a consistent way to
 * configure token requests.
 */
interface IAcquireTokenParameters {

    /**
     * Gets the set of scopes (permissions) requested for the access token.
     * <p>
     * Scopes represent the permissions that the application is requesting access to.
     *
     * @return A set of scope strings being requested for the access token.
     */
    Set<String> scopes();

    /**
     * Gets the claims request parameter that will be sent with the authentication request.
     * <p>
     * The claims request parameter can be used to request specific claims to be returned in the token,
     * to request MFA authentication, or to control other aspects of token issuance.
     *
     * @return A {@link ClaimsRequest} object containing the requested claims, or null if no claims are specified.
     */
    ClaimsRequest claims();

    /**
     * Gets additional HTTP headers to be included in the token request.
     * <p>
     * These headers will be added to the HTTP requests sent to the token endpoint.
     * This can be useful for scenarios requiring custom headers for proxies or for diagnostic purposes.
     *
     * @return A map of additional HTTP headers to include with the token request, or null if no extra headers are needed.
     */
    Map<String, String> extraHttpHeaders();

    /**
     * Gets the tenant identifier for the token request.
     * <p>
     * When specified, this value overrides the tenant specified in the application's authority URL.
     * It can be a tenant ID (GUID), domain name, or one of the special values like "common" or "organizations".
     *
     * @return The tenant identifier to use for the token request, or null to use the tenant from the authority URL.
     */
    String tenant();

    /**
     * Gets additional query parameters to be included in the token request.
     * <p>
     * These parameters will be added to the query string in requests to the authorization and token endpoints.
     * This can be useful for scenarios requiring custom parameters that aren't explicitly supported in the library.
     *
     * @return A map of additional query parameters to include with the token request, or null if no extra parameters are needed.
     *
     * @deprecated Not recommended for production scenarios. It will be removed in a future release, and the behavior may be replaced by a new API.
     */
    @Deprecated
    Map<String, String> extraQueryParameters();

    /**
     * Gets the client-originated claims (a raw JSON string) set via the per-request
     * {@code claimsFromClient(...)} builder method.
     * <p>
     * Unlike {@link #claims()} (server-issued claims challenges, which bypass/refresh the cache),
     * client claims are cached and the cache entry is keyed on the claims value, and they are sent on
     * the wire as a standard OAuth {@code claims} parameter.
     *
     * @return the client-originated claims JSON, or null if none were provided.
     */
    default String clientClaims() {
        return null;
    }

    /**
     * Computes the extended cache-key hash contributed by this request's parameters (for example
     * {@code fmi_path} or {@code client_claims}). The hash isolates cache entries so that requests
     * with different component values do not collide. Returns an empty string when there are no
     * extended cache-key components.
     *
     * @return the Base64URL-encoded SHA-256 hash of the cache-key components, or an empty string.
     */
    default String computeExtCacheKeyHash() {
        return "";
    }
}
