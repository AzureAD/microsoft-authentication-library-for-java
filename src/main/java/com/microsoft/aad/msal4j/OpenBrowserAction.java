// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URL;

/**
 * Interface to be implemented to override system browser initialization logic. Otherwise,
 * PublicClientApplication defaults to using default system browser
 */
public interface OpenBrowserAction {
    void openBrowser(URL url);
}
