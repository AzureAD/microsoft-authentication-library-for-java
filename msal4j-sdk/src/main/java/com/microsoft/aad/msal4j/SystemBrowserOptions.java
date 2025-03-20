// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URI;

/**
 * Options for using the default OS browser as a separate process to handle interactive authentication.
 * MSAL will listen for the OS browser to finish authenticating, but it cannot close the browser.
 * It can however response with a HTTP 200 OK message or a 302 Redirect, which can be configured here.
 * For more details, see https://aka.ms/msal4j-interactive-request
 */
public class SystemBrowserOptions {

    private String htmlMessageSuccess;
    private String htmlMessageError;
    private URI browserRedirectSuccess;
    private URI browserRedirectError;
    private OpenBrowserAction openBrowserAction;

    private SystemBrowserOptions(String htmlMessageSuccess, String htmlMessageError, URI browserRedirectSuccess, URI browserRedirectError, OpenBrowserAction openBrowserAction) {
        this.htmlMessageSuccess = htmlMessageSuccess;
        this.htmlMessageError = htmlMessageError;
        this.browserRedirectSuccess = browserRedirectSuccess;
        this.browserRedirectError = browserRedirectError;
        this.openBrowserAction = openBrowserAction;
    }

    /**
     * Builder for {@link SystemBrowserOptions}
     */
    public static SystemBrowserOptionsBuilder builder() {
        return new SystemBrowserOptionsBuilder();
    }

    public String htmlMessageSuccess() {
        return this.htmlMessageSuccess;
    }

    public String htmlMessageError() {
        return this.htmlMessageError;
    }

    public URI browserRedirectSuccess() {
        return this.browserRedirectSuccess;
    }

    public URI browserRedirectError() {
        return this.browserRedirectError;
    }

    public OpenBrowserAction openBrowserAction() {
        return this.openBrowserAction;
    }

    public static class SystemBrowserOptionsBuilder {
        private String htmlMessageSuccess;
        private String htmlMessageError;
        private URI browserRedirectSuccess;
        private URI browserRedirectError;
        private OpenBrowserAction openBrowserAction;

        SystemBrowserOptionsBuilder() {
        }

        /**
         * When the user finishes authenticating, MSAL will respond with a Http 200 OK message, which the
         * browser will show to the user
         */
        public SystemBrowserOptionsBuilder htmlMessageSuccess(String htmlMessageSuccess) {
            this.htmlMessageSuccess = htmlMessageSuccess;
            return this;
        }

        /**
         * WHen the user finishes authenticating, but an error occurred, MSAL will respond with a
         * Http 200 Ok message, which the browser will show to the user.
         */
        public SystemBrowserOptionsBuilder htmlMessageError(String htmlMessageError) {
            this.htmlMessageError = htmlMessageError;
            return this;
        }

        /**
         * When the user finishes authenticating, MSAL will redirect the browser to the given URI.
         * Takes precedence over htmlMessageSuccess
         */
        public SystemBrowserOptionsBuilder browserRedirectSuccess(URI browserRedirectSuccess) {
            this.browserRedirectSuccess = browserRedirectSuccess;
            return this;
        }

        /**
         * When the the user finishes authenticating, but an error occurred, MSAL will redirect the
         * browser to the given URI.
         * Takes precedence over htmlMessageError
         */
        public SystemBrowserOptionsBuilder browserRedirectError(URI browserRedirectError) {
            this.browserRedirectError = browserRedirectError;
            return this;
        }

        /**
         * Allows developers to implement their own logic for starting a browser and navigating to a
         * specific Uri. Msal will use this when opening the browser. If not set, the user configured
         * browser will be used.
         */
        public SystemBrowserOptionsBuilder openBrowserAction(OpenBrowserAction openBrowserAction) {
            this.openBrowserAction = openBrowserAction;
            return this;
        }

        public SystemBrowserOptions build() {
            return new SystemBrowserOptions(this.htmlMessageSuccess, this.htmlMessageError, this.browserRedirectSuccess, this.browserRedirectError, this.openBrowserAction);
        }

        public String toString() {
            return "SystemBrowserOptions.SystemBrowserOptionsBuilder(htmlMessageSuccess=" + this.htmlMessageSuccess + ", htmlMessageError=" + this.htmlMessageError + ", browserRedirectSuccess=" + this.browserRedirectSuccess + ", browserRedirectError=" + this.browserRedirectError + ", openBrowserAction=" + this.openBrowserAction + ")";
        }
    }
}
