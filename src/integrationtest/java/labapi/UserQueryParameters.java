// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode
public class UserQueryParameters {

    public static final String USER_TYPE = "usertype";
    public static final String MFA = "mfa";
    public static final String PROTECTION_POLICEY = "protectionpolicy";
    public static final String HOME_DOMAIN = "homedomain";
    public static final String HOME_UPN = "homeupn";
    public static final String B2C_PROVIDER = "b2cprovider";
    public static final String FEDERATION_PROVIDER = "federationprovider";
    public static final String AZURE_ENVIRONMENT = "azureenvironment";
    public static final String SIGN_IN_AUDIENCE = "signinaudience";

    public Map<String, String> parameters = new HashMap<>();
}
