// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.JWTClaimsSet;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

class IdToken {

    static final String ISSUER = "iss";
    static final String SUBJECT = "sub";
    static final String AUDIENCE = "aud";
    static final String EXPIRATION_TIME = "exp";
    static final String ISSUED_AT = "issuedAt";
    static final String NOT_BEFORE = "nbf";
    static final String NAME = "name";
    static final String PREFERRED_USERNAME = "preferred_username";
    static final String OBJECT_IDENTIFIER = "oid";
    static final String TENANT_IDENTIFIER = "tid";
    static final String UPN = "upn";
    static final String UNIQUE_NAME = "unique_name";

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

    static IdToken createFromJWTClaims(final JWTClaimsSet claims) throws ParseException {
        IdToken idToken = new IdToken();

        idToken.issuer = claims.getStringClaim(ISSUER);
        idToken.subject = claims.getStringClaim(SUBJECT);
        idToken.audience = claims.getStringClaim(AUDIENCE);

        idToken.expirationTime = claims.getLongClaim(EXPIRATION_TIME);
        idToken.issuedAt = claims.getLongClaim(ISSUED_AT);
        idToken.notBefore = claims.getLongClaim(NOT_BEFORE);

        idToken.name = claims.getStringClaim(NAME);
        idToken.preferredUsername = claims.getStringClaim(PREFERRED_USERNAME);
        idToken.objectIdentifier = claims.getStringClaim(OBJECT_IDENTIFIER);
        idToken.tenantIdentifier = claims.getStringClaim(TENANT_IDENTIFIER);

        idToken.upn = claims.getStringClaim(UPN);
        idToken.uniqueName = claims.getStringClaim(UNIQUE_NAME);

        return idToken;
    }
}
