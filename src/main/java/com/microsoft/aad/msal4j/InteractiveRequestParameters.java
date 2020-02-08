// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.URI;
import java.net.URL;
import java.util.Set;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotEmpty;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Object containing parameters for interactive requests. Can be used as parameter to
 * {@link PublicClientApplication#acquireToken(InteractiveRequestParameters)}
 */
@Builder
@Accessors(fluent = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class InteractiveRequestParameters {

    @NonNull
    private Set<String> scopes;

    @Setter(AccessLevel.PACKAGE)
    @NonNull
    private URI redirectUri;

    /**
     * Sets system browser options to be used by the PublicClientApplication
     * @param systemBrowserOptions System browser options when using acquireTokenInteractiveRequest
     * @return instance of the Builder on which method was called
     */
    private SystemBrowserOptions systemBrowserOptions;

    private static InteractiveRequestParametersBuilder builder() {
        return new InteractiveRequestParametersBuilder();
    }

    public static InteractiveRequestParametersBuilder builder(Set<String> scopes, URI redirectUri) {

        validateNotEmpty("scopes", scopes);
        validateNotNull("redirect_uri", redirectUri);

        return builder()
                .scopes(scopes)
                .redirectUri(redirectUri);
    }
}
