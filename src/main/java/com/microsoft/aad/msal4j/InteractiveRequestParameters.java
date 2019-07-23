package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import java.net.URI;
import java.util.Set;

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

    @NonNull
    private URI redirectUri;

    /**
     * Sets system browser options to be used by the PublicClientApplication
     * @param systemBrowserOptions System browser options when using acquireTokenInteractiveRequest
     * @return instance of the Builder on which method was called
     */
    private SystemBrowserOptions systemBrowserOptions;


}
