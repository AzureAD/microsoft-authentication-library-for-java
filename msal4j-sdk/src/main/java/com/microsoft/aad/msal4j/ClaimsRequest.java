// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the claims request parameter as an object
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
public class ClaimsRequest implements JsonSerializable<ClaimsRequest> {

    List<RequestedClaim> idTokenRequestedClaims = new ArrayList<>();
    List<RequestedClaim> userInfoRequestedClaims = new ArrayList<>();
    List<RequestedClaim> accessTokenRequestedClaims = new ArrayList<>();

    /**
     * Inserts a claim into the list of claims to be added to the "id_token" section of an OIDC claims request
     *
     * @param claim                        the name of the claim to be requested
     * @param requestedClaimAdditionalInfo additional information about the claim being requested
     */
    public void requestClaimInIdToken(String claim, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
        idTokenRequestedClaims.add(new RequestedClaim(claim, requestedClaimAdditionalInfo));
    }

    /**
     * Inserts a claim into the list of claims to be added to the "userinfo" section of an OIDC claims request
     *
     * @param claim                        the name of the claim to be requested
     * @param requestedClaimAdditionalInfo additional information about the claim being requested
     */
    protected void requestClaimInUserInfo(String claim, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
        userInfoRequestedClaims.add(new RequestedClaim(claim, requestedClaimAdditionalInfo));
    }

    /**
     * Inserts a claim into the list of claims to be added to the "access_token" section of an OIDC claims request
     *
     * @param claim                        the name of the claim to be requested
     * @param requestedClaimAdditionalInfo additional information about the claim being requested
     */
    protected void requestClaimInAccessToken(String claim, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
        accessTokenRequestedClaims.add(new RequestedClaim(claim, requestedClaimAdditionalInfo));
    }

    /**
     * Converts the ClaimsRequest object to a JSON-formatted String which follows the specification for the OIDC claims request parameter
     *
     * @return a String following JSON formatting
     */
    public String formatAsJSONString() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             JsonWriter jsonWriter = JsonProviders.createWriter(outputStream)) {
            toJson(jsonWriter);

            jsonWriter.flush();
            return outputStream.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new MsalClientException("Could not convert ClaimsRequest to string: " + e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        writeClaimsToJsonWriter(jsonWriter, "id_token", idTokenRequestedClaims);
        writeClaimsToJsonWriter(jsonWriter, "userinfo", userInfoRequestedClaims);
        writeClaimsToJsonWriter(jsonWriter, "access_token", accessTokenRequestedClaims);

        jsonWriter.writeEndObject();
        return jsonWriter;
    }

    private void writeClaimsToJsonWriter(JsonWriter jsonWriter, String sectionName, List<RequestedClaim> claims) throws IOException {
        if (claims.isEmpty()) {
            return;
        }

        jsonWriter.writeStartObject(sectionName);

        for (RequestedClaim claim : claims) {
            if (claim.name != null) {
                if (claim.getRequestedClaimAdditionalInfo() != null) {
                    jsonWriter.writeJsonField(claim.name,  claim.getRequestedClaimAdditionalInfo());
                } else {
                    jsonWriter.writeNullField(claim.name);
                }
            }
        }

        jsonWriter.writeEndObject();
    }

    /**
     * Creates an instance of ClaimsRequest from a JSON-formatted String which follows the specification for the OIDC claims request parameter
     *
     * @param claims a String following JSON formatting
     * @return a ClaimsRequest instance
     */
    public static ClaimsRequest formatAsClaimsRequest(String claims) {
        try (JsonReader jsonReader = JsonProviders.createReader(claims)) {
            ClaimsRequest claimsRequest = new ClaimsRequest();

            return jsonReader.readObject(reader -> {
                if (reader.currentToken() != JsonToken.START_OBJECT) {
                    throw new IllegalStateException("Expected start of object but was " + reader.currentToken());
                }

                while (reader.nextToken() != JsonToken.END_OBJECT) {
                    parseClaims(reader, claimsRequest, reader.getFieldName());
                }

                return claimsRequest;
            });
        } catch (IOException e) {
            throw new MsalClientException("Could not convert string to ClaimsRequest: " + e.getMessage(),
                    AuthenticationErrorCode.INVALID_JSON);
        }
    }

    private static void parseClaims(JsonReader jsonReader, ClaimsRequest claimsRequest, String section) throws IOException {
        if (jsonReader.currentToken() != JsonToken.FIELD_NAME) {
            jsonReader.nextToken();
        }

        jsonReader.nextToken();
        if (jsonReader.currentToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("Expected start of object but was " + jsonReader.currentToken());
        }

        while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
            String claimName = jsonReader.getFieldName();
            jsonReader.nextToken();

            RequestedClaimAdditionalInfo claimInfo = null;
            if (jsonReader.currentToken() == JsonToken.START_OBJECT) {
                boolean essential = false;
                String value = null;
                List<String> values = null;

                while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = jsonReader.getFieldName();
                    jsonReader.nextToken();

                    switch (fieldName) {
                        case "essential": essential = jsonReader.getBoolean(); break;
                        case "value": value = jsonReader.getString(); break;
                        case "values":
                            values = new ArrayList<>();
                            if (jsonReader.currentToken() == JsonToken.START_ARRAY) {
                                while (jsonReader.nextToken() != JsonToken.END_ARRAY) {
                                    values.add(jsonReader.getString());
                                }
                            }
                            break;
                        default: jsonReader.skipChildren(); break;
                    }
                }

                if (essential || value != null || values != null) {
                    claimInfo = new RequestedClaimAdditionalInfo(essential, value, values);
                }
            }

            switch (section) {
                case "access_token": claimsRequest.requestClaimInAccessToken(claimName, claimInfo); break;
                case "id_token": claimsRequest.requestClaimInIdToken(claimName, claimInfo); break;
                case "userinfo": claimsRequest.requestClaimInUserInfo(claimName, claimInfo); break;
            }
        }
    }

    public List<RequestedClaim> getIdTokenRequestedClaims() {
        return this.idTokenRequestedClaims;
    }

    public void setIdTokenRequestedClaims(List<RequestedClaim> idTokenRequestedClaims) {
        this.idTokenRequestedClaims = idTokenRequestedClaims;
    }
}