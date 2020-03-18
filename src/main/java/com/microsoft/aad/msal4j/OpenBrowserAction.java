// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URL;

/**
 * Interface to be implemented to override system browser initialization logic. Otherwise,
 * PublicClientApplication defaults to using default system browser
 */
public interface OpenBrowserAction {

    /**
     * Override for providing custom browser initialization logic. Method that is called by MSAL
     * when doing {@link IPublicClientApplication#acquireToken(InteractiveRequestParameters)}. If
     * not overridden, MSAL will attempt to open URL in default system browser.
     * @param url URL to the /authorize endpoint which should opened up in a browser so the user can
     *            provide their credentials and consent to scopes.
     */
    void openBrowser(URL url);
}
