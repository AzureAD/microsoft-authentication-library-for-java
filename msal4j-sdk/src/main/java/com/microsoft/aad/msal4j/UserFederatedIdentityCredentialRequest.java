// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.LinkedHashMap;
import java.util.Map;

class UserFederatedIdentityCredentialRequest extends MsalRequest {

    UserFederatedIdentityCredentialParameters parameters;

    UserFederatedIdentityCredentialRequest(UserFederatedIdentityCredentialParameters parameters,
                                           ConfidentialClientApplication application,
                                           RequestContext requestContext) {
        super(application, createMsalGrant(parameters), requestContext);
        this.parameters = parameters;
    }

    private static OAuthAuthorizationGrant createMsalGrant(UserFederatedIdentityCredentialParameters parameters) {
        Map<String, String> params = new LinkedHashMap<>();

        params.put(GrantConstants.GRANT_TYPE_PARAMETER, GrantConstants.USER_FIC);
        params.put(GrantConstants.USER_FEDERATED_IDENTITY_CREDENTIAL, parameters.assertion());

        // Mutually exclusive: user_id (by OID) or username (by UPN)
        if (parameters.userObjectId() != null) {
            params.put(GrantConstants.USER_ID_PARAMETER, parameters.userObjectId().toString());
        } else {
            params.put(GrantConstants.USERNAME_PARAMETER, parameters.username());
        }

        if (parameters.claims() != null) {
            params.put("claims", parameters.claims().formatAsJSONString());
        }

        // OAuthAuthorizationGrant constructor automatically adds:
        // - scope augmented with openid, offline_access, profile (COMMON_SCOPES)
        // - client_info=1
        return new OAuthAuthorizationGrant(params, parameters.scopes());
    }

    UserFederatedIdentityCredentialParameters parameters() {
        return this.parameters;
    }
}
