// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.nimbusds.jwt.JWTClaimsSet;

import java.io.IOException;
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

    public static IdToken convertJsonToObject(String json) throws IOException{

        IdToken idToken = new IdToken();
        if (json != null) {

            JsonFactory jsonFactory = new JsonFactory();
            try (JsonParser jsonParser = jsonFactory.createParser(json)) {

                if (jsonParser.nextToken().equals(JsonToken.START_ARRAY)) {
                    jsonParser.nextToken();
                }

                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldname = jsonParser.getCurrentName();
                    if ("iss".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.issuer = jsonParser.getText();
                    }

                    if ("sub".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.subject = jsonParser.getText();

                    }

                    if ("aud".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.audience = jsonParser.getText();

                    }

                    if ("exp".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.expirationTime = jsonParser.getLongValue();

                    }

                    if ("iat".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.issuedAt = jsonParser.getLongValue();

                    }

                    if ("nbf".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.notBefore = jsonParser.getLongValue();

                    }

                    if ("name".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.name = jsonParser.getText();

                    }

                    if ("preferred_username".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.preferredUsername = jsonParser.getText();

                    }

                    if ("oid".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.objectIdentifier = jsonParser.getText();

                    }

                    if ("tid".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.tenantIdentifier = jsonParser.getText();

                    }

                    if ("upn".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.upn = jsonParser.getText();
                    }

                    if ("unique_name".equals(fieldname)) {
                        jsonParser.nextToken();
                        idToken.uniqueName = jsonParser.getText();
                    }
                }
            }

        }
         return idToken;

    }
}
