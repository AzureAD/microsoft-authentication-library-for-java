// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Details about the cause of an {@link MsalInteractionRequiredException}, giving a hint about the
 * user can expect when they go through interactive authentication
 */
public enum InteractionRequiredExceptionReason {

    /**
     * No further details are provided. It is possible that the user will be able to resolve the issue
     * by launching interactive authentication
     */
    NONE("none"),

    /**
     * Issue cannot be resolved at this time. Launching interactive authentication flow will show a
     * message explaining the condition
     */
    MESSAGE_ONLY("message_only"),

    /**
     * Issue can be resolved by user interaction during the interactive authentication flow.
     */
    BASIC_ACTION("basic_action"),

    /**
     * Issue can be resolved by remedial interaction with the system, outside of the interactive
     * authentication flow. Starting an interactive authentication flow will show the user what they
     * need to to do, but it is possible that the user is unable to complete the action
     */
    ADDITIONAL_ACTION("additional_action"),

    /**
     * User consent is missing, or has been revoked. Issue can be resolved by user consenting during
     * the interactive authentication flow
     */
    CONSENT_REQUIRED("consent_required"),

    /**
     * User's password has expired. Issue can be resolved by user during the interactive authentication
     * flow
     */
    USER_PASSWORD_EXPIRED("user_password_expired");

    private String error;

    InteractionRequiredExceptionReason(String error) {
        this.error = error;
    }

    static InteractionRequiredExceptionReason fromSubErrorString(String subError) {
        if (StringHelper.isBlank(subError)) {
            return NONE;
        }

        for (InteractionRequiredExceptionReason reason :
                InteractionRequiredExceptionReason.values()) {
            if (reason.error.equalsIgnoreCase(subError)) {
                return reason;
            }
        }
        return NONE;
    }
}
