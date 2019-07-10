// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import com.nimbusds.jwt.JWTClaimsSet;

import java.text.ParseException;

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

    @SerializedName("iss")
    protected String issuer;

    @SerializedName("sub")
    protected String subject;

    @SerializedName("aud")
    protected String audience ;

    @SerializedName("exp")
    protected Long expirationTime;

    @SerializedName("iat")
    protected Long issuedAt;

    @SerializedName("nbf")
    protected Long notBefore;

    @SerializedName("name")
    protected String name;

    @SerializedName("preferred_username")
    protected String preferredUsername;

    @SerializedName("oid")
    protected String objectIdentifier;

    @SerializedName("tid")
    protected String tenantIdentifier;

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

        return idToken;
    }
}
