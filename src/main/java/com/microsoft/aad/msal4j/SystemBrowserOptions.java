// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.URI;

/**
 * Options for using the default OS browser as a separate process to handle interactive authentication.
 *  MSAL will listen for the OS browser to finish authenticating, but it cannot close the browser.
 *  It can however response with a HTTP 200 OK message or a 302 Redirect, which can be configured here.
 *  For more details, see https://aka.ms/msal4j-os-browser
 */
@Accessors(fluent = true)
@Getter
@Setter
public class SystemBrowserOptions {

    /**
     * When the user finishes authenticating, MSAL will respond with a Http 200 OK message, which the
     * browser will show to the user
     */
    private String htmlMessageSuccess;

    /**
     * WHen the user finishes authenticating, but an error occurred, MSAL will respond with a
     * Http 200 Ok message, which the browser will show to the user.
     */
    private String htmlMessageError;

    /**
     * When the user finishes authenticating, MSAL will redirect the browser to the given URI.
     * Takes precedence over htmlMessageSuccess
     */
    private URI browserRedirectSuccess;

    /**
     * When the the user finishes authenticating, but an error occurred, MSAL will redirect the
     * browser to the given URI.
     * Takes precedence over htmlMessageError
     */
    private URI browserRedirectError;

    /**
     * Allows developers to implement their own logic for starting a browser and navigating to a
     * specific Uri. Msal will use this when opening the browser. If not set, the user configured
     * browser will be used.
     */
    private OpenBrowserAction openBrowserAction;
}
