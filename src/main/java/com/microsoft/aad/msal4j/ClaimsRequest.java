// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Represents the claims request parameter as an object
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
@Getter
@Setter
public class ClaimsRequest {
    List<RequestedClaim> userInfoRequestedClaims = new ArrayList<>();
    List<RequestedClaim> idTokenRequestedClaims = new ArrayList<>();
    List<RequestedClaim> accessTokenRequestedClaims = new ArrayList<>();

    /**
     * Inserts a claim into the list of claims to be added to the "userinfo" section of an OIDC claims request
     *
     * @param claim the name of the claim to be requested
     * @param requestedClaimAdditionalInfo additional information about the claim being requested
     */
    public void requestClaimInUserInfo(String claim, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
        userInfoRequestedClaims.add(new RequestedClaim(claim, requestedClaimAdditionalInfo));
    }

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
    public void requestClaimInAccessToken(String claim, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
        accessTokenRequestedClaims.add(new RequestedClaim(claim, requestedClaimAdditionalInfo));
    }

    /**
     * Converts the ClaimsRequest object to a JSON-formatted String which follows the specification for the OIDC claims request parameter
     *
     * @return a String following JSON formatting
     */
    public String formatAsJSONString() {
        String jsonString = "";
        if (!userInfoRequestedClaims.isEmpty()) {
            jsonString += String.format("\"userinfo\":%s", convertClaimsListToString(userInfoRequestedClaims));
        }

        if (!idTokenRequestedClaims.isEmpty()) {
            if (jsonString.length() > 0) jsonString += ",";
            jsonString += String.format("\"id_token\":%s", convertClaimsListToString(idTokenRequestedClaims));
        }

        if (!accessTokenRequestedClaims.isEmpty()) {
            if (jsonString.length() > 0) jsonString += ",";
            jsonString += String.format("\"access_token\":%s", convertClaimsListToString(accessTokenRequestedClaims));
        }

        return String.format("{%s}", jsonString);
    }

    private String convertClaimsListToString(List<RequestedClaim> claims) {
        //Converts a given list to a string, following a pattern of {item1,item2,...,itemN}
        StringJoiner combinedStrings = new StringJoiner(",","{","}");

        for (RequestedClaim claim : claims) {
            combinedStrings.add(claim.formatAsJSONString());
        }

        return combinedStrings.toString();
    }
}
