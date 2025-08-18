// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import java.util.HashMap;
import java.util.Map;

public class UserQueryParameters {

    public static final String USER_TYPE = "usertype";
    public static final String B2C_PROVIDER = "b2cprovider";
    public static final String FEDERATION_PROVIDER = "federationprovider";
    public static final String AZURE_ENVIRONMENT = "azureenvironment";
    public static final String HOME_AZURE_ENVIRONMENT = "guesthomeazureenvironment";
    public static final String GUEST_HOME_DIN = "guesthomedin";
    public static final String SIGN_IN_AUDIENCE = "signInAudience";

    public Map<String, String> parameters = new HashMap<>();
}
