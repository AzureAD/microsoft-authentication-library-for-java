// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;

import java.util.Iterator;
import java.io.IOException;
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
     * @param claim the name of the claim to be requested
     * @param requestedClaimAdditionalInfo additional information about the claim being requested
     */
    public void requestClaimInIdToken(String claim, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
        idTokenRequestedClaims.add(new RequestedClaim(claim, requestedClaimAdditionalInfo));
    }

    /**

     * Inserts a claim into the list of claims to be added to the "userinfo" section of an OIDC claims request
     *
     * @param claim the name of the claim to be requested
     * @param requestedClaimAdditionalInfo additional information about the claim being requested
     */
    protected void requestClaimInUserInfo(String claim, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
        userInfoRequestedClaims.add(new RequestedClaim(claim, requestedClaimAdditionalInfo));
    }

    /**
     * Inserts a claim into the list of claims to be added to the "access_token" section of an OIDC claims request
     *
     * @param claim the name of the claim to be requested
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
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        if (!idTokenRequestedClaims.isEmpty()) {
            rootNode.set("id_token", convertClaimsToObjectNode(idTokenRequestedClaims));
        }
        if (!userInfoRequestedClaims.isEmpty()) {
            rootNode.set("userinfo", convertClaimsToObjectNode(userInfoRequestedClaims));
        }
        if (!accessTokenRequestedClaims.isEmpty()) {
            rootNode.set("access_token", convertClaimsToObjectNode(accessTokenRequestedClaims));
        }

        return mapper.valueToTree(rootNode).toString();
    }

    private ObjectNode convertClaimsToObjectNode(List<RequestedClaim> claims) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode claimsNode = mapper.createObjectNode();


        for (RequestedClaim claim : claims) {
            claimsNode.setAll((ObjectNode) mapper.valueToTree(claim));
        }
        return claimsNode;
    }

    /**
     * Creates an instance of ClaimsRequest from a JSON-formatted String which follows the specification for the OIDC claims request parameter
     *
     * @param claims a String following JSON formatting
     * @return a ClaimsRequest instance
     */
    public static ClaimsRequest formatAsClaimsRequest(String claims) {
        try {
            ClaimsRequest cr = new ClaimsRequest();

            ObjectMapper mapper = new ObjectMapper();
            ObjectReader reader = mapper.readerFor(new TypeReference<List<String>>() {});

            JsonNode jsonClaims = mapper.readTree(claims);

            addClaimsFromJsonNode(jsonClaims.get("id_token"), "id_token", cr, reader);
            addClaimsFromJsonNode(jsonClaims.get("userinfo"), "userinfo", cr, reader);
            addClaimsFromJsonNode(jsonClaims.get("access_token"), "access_token", cr, reader);

            return cr;
        } catch (IOException e) {
            throw new MsalClientException("Could not convert string to ClaimsRequest: " + e.getMessage(), AuthenticationErrorCode.INVALID_JSON);
        }
    }

    private static void addClaimsFromJsonNode(JsonNode claims, String group, ClaimsRequest cr, ObjectReader reader) throws IOException {
        Iterator<String> claimsIterator;

        if (claims != null) {
            claimsIterator = claims.fieldNames();
            while (claimsIterator.hasNext()) {
                String claim = claimsIterator.next();
                Boolean essential = null;
                String value = null;
                List<String> values = null;
                RequestedClaimAdditionalInfo claimInfo = null;

                if (claims.get(claim).has("essential")) essential = claims.get(claim).get("essential").asBoolean();
                if (claims.get(claim).has("value")) value = claims.get(claim).get("value").textValue();
                if (claims.get(claim).has("values")) values = reader.readValue(claims.get(claim).get("values"));

                //'null' is a valid value for RequestedClaimAdditionalInfo, so only initialize it if one of the parameters is not null
                if (essential != null || value != null || values != null) {
                    claimInfo = new RequestedClaimAdditionalInfo(essential == null ? false : essential, value, values);
                }

                if (group.equals("id_token")) cr.requestClaimInIdToken(claim, claimInfo);
                if (group.equals("userinfo")) cr.requestClaimInUserInfo(claim, claimInfo);
                if (group.equals("access_token")) cr.requestClaimInAccessToken(claim, claimInfo);
            }
        }
    }
}
