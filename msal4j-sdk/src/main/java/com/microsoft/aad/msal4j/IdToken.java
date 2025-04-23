// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

class IdToken implements Serializable {

    @JsonProperty("iss")
    protected String issuer;

    @JsonProperty("sub")
    protected String subject;

    @JsonProperty("aud")
    protected String audience;

    @JsonProperty("exp")
    protected Long expirationTime;

    @JsonProperty("iat")
    protected Long issuedAt;

    @JsonProperty("nbf")
    protected Long notBefore;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("preferred_username")
    protected String preferredUsername;

    @JsonProperty("oid")
    protected String objectIdentifier;

    @JsonProperty("tid")
    protected String tenantIdentifier;

    @JsonProperty("upn")
    protected String upn;

    @JsonProperty("unique_name")
    protected String uniqueName;
}
