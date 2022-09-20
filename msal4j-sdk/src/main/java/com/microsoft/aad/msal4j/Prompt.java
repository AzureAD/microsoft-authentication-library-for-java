// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Indicate the type of user interaction that is required when sending authorization code request.
 */
public enum Prompt {

    /**
     * The user should be prompted to reauthenticate.
     */
    LOGIN("login"),

    /**
     * The user is prompted to select an account, interrupting single sign on. The user may select
     * an existing signed-in account, enter their credentials for a remembered account,
     * or choose to use a different account altogether.
     */
    SELECT_ACCOUNT("select_account"),

    /**
     * User consent has been granted, but needs to be updated. The user should be prompted to consent.
     */
    CONSENT("consent"),

    /**
     * An administrator should be prompted to consent on behalf of all users in their organization.
     * <p>
     * Deprecated, instead use Prompt.ADMIN_CONSENT
     */
    @Deprecated
    ADMING_CONSENT("admin_consent"),

    /**
     * An administrator should be prompted to consent on behalf of all users in their organization.
     */
    ADMIN_CONSENT("admin_consent"),

    /**
     * User will not be shown an interactive prompt
     */
    NONE("none");

    private String prompt;

    Prompt(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public String toString() {
        return prompt;
    }
}
