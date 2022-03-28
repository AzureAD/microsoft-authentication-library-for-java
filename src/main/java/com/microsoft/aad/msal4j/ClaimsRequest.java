// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;


import com.fasterxml.jackson.core.*;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the claims request parameter as an object
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
public class ClaimsRequest {

    @Getter
    @Setter
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

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        JsonFactory jsonFactory = new JsonFactory();
        try {
            try (JsonGenerator jsonGenerator = jsonFactory
                    .createGenerator(stream, JsonEncoding.UTF8)) {

                jsonGenerator.writeStartObject();

                if (!idTokenRequestedClaims.isEmpty()) {
                    jsonGenerator.writeObjectFieldStart("id_token");
                    updateStream(jsonGenerator, idTokenRequestedClaims);
                    jsonGenerator.writeEndObject();
                }

                if (!userInfoRequestedClaims.isEmpty()) {
                    jsonGenerator.writeObjectFieldStart("userinfo");
                    updateStream(jsonGenerator, userInfoRequestedClaims);
                    jsonGenerator.writeEndObject();
                }

                if (!accessTokenRequestedClaims.isEmpty()) {
                    jsonGenerator.writeObjectFieldStart("access_token");
                    updateStream(jsonGenerator, accessTokenRequestedClaims);
                    jsonGenerator.writeEndObject();
                }


                jsonGenerator.writeEndObject();
                jsonGenerator.close();
            }
            return new String(stream.toByteArray(), "UTF-8");

        } catch (IOException e) {
            e.printStackTrace();

            return null;
        } finally {

        }
    }

    private void updateStream(JsonGenerator jsonGenerator, List<RequestedClaim> claims) throws IOException {

        for(RequestedClaim claim: claims){
            if(claim.getRequestedClaimAdditionalInfo()!=null){
                jsonGenerator.writeObjectFieldStart(claim.getName());
                if (claim.getRequestedClaimAdditionalInfo().isEssential())
                    jsonGenerator.writeBooleanField("essential", claim.getRequestedClaimAdditionalInfo().isEssential());
                if (claim.getRequestedClaimAdditionalInfo().getValue() != null)
                    jsonGenerator.writeStringField("value", claim.getRequestedClaimAdditionalInfo().getValue());
                if (claim.getRequestedClaimAdditionalInfo().getValues() != null) {
                    StringBuilder concatenatedValue = new StringBuilder();
                    List<String> list = new ArrayList<>();
                    for (String value : claim.getRequestedClaimAdditionalInfo().getValues()) {
                        list.add(value);
                    }
                    String[] stringArray = new String[list.size()];
                    list.toArray(stringArray);
                    jsonGenerator.writeFieldName("values");
                    jsonGenerator.writeArray(stringArray, 0, stringArray.length);
                }
                jsonGenerator.writeEndObject();
            }else {
                jsonGenerator.writeNullField(claim.getName());
            }

        }
    }

    /**
     * Creates an instance of ClaimsRequest from a JSON-formatted String which follows the specification for the OIDC claims request parameter
     *
     * @param claims a String following JSON formatting
     * @return a ClaimsRequest instance
     */

    public static ClaimsRequest formatAsClaimsRequest(String claims) {
        try {
            ClaimsRequest claimsRequest = new ClaimsRequest();

            JsonFactory jsonFactory = new JsonFactory();
            try (JsonParser jsonParser = jsonFactory.createParser(claims)) {

                if (jsonParser.nextToken().equals(JsonToken.START_OBJECT)) {
                    jsonParser.nextToken();
                }
                JsonToken nextToken = jsonParser.nextToken();
                while (nextToken != JsonToken.END_OBJECT && nextToken != null) {
                    String fieldName = jsonParser.getCurrentName();
                    if (("id_token").equalsIgnoreCase(fieldName)) {
                        claimsRequest.idTokenRequestedClaims = getTokenList(jsonParser);
                        jsonParser.nextToken();
                    } else if (("access_token").equalsIgnoreCase(fieldName)) {
                        claimsRequest.accessTokenRequestedClaims = getTokenList(jsonParser);
                        jsonParser.nextToken();
                    } else if (("userinfo").equalsIgnoreCase(fieldName)) {
                        claimsRequest.userInfoRequestedClaims = getTokenList(jsonParser);
                        jsonParser.nextToken();
                    }
                    nextToken = jsonParser.nextToken();
                }
            }
            return claimsRequest;
        } catch (IOException e) {
            throw new MsalClientException("Could not convert string to ClaimsRequest: " + e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    private static List<RequestedClaim> getTokenList(JsonParser jsonParser) throws IOException {
        List<RequestedClaim> list = new ArrayList<>();
        while(jsonParser.nextToken()!=JsonToken.END_OBJECT) {
            String name = jsonParser.getValueAsString();
            jsonParser.nextToken();
            String fieldName;
            RequestedClaimAdditionalInfo requestedClaimAdditionalInfo = new RequestedClaimAdditionalInfo();
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                fieldName = jsonParser.getCurrentName();
                if ("essential".equals(fieldName)) {
                    jsonParser.nextToken();
                    requestedClaimAdditionalInfo.setEssential(jsonParser.getBooleanValue());
                }
                if ("value".equals(fieldName)) {
                    jsonParser.nextToken();
                    requestedClaimAdditionalInfo.setValue(jsonParser.getText());
                }
                if ("values".equals(fieldName)) {
                    jsonParser.nextToken();
                    jsonParser.nextToken();

                    List<String> listOfValues = new ArrayList<>();
                    listOfValues.add(jsonParser.getText());
                    jsonParser.nextToken();
                    requestedClaimAdditionalInfo.setValues(listOfValues);
                }
            }
            RequestedClaim requestedClaim = new RequestedClaim(name, requestedClaimAdditionalInfo);
            list.add(requestedClaim);
        }
        return list;
    }
}
