// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
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

        for (RequestedClaim claim: claims) {
            claimsNode.setAll((ObjectNode) mapper.valueToTree(claim));
        }
        return claimsNode;
    }
}
