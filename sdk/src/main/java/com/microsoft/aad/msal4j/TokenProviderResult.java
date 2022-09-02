// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/// Token result from external token provider
public class TokenProviderResult {

    //Access token - mandatory
    private String accessToken;
    //tenant Id of the client application
    private String tenantId;
    //Expiration of the token - mandatory
    private long expiresInSeconds;
    //When the token be refreshed proactively (optional)
    private long refreshInSeconds;

}
