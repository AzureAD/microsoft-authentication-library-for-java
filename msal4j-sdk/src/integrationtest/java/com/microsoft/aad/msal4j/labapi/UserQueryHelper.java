// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

import com.microsoft.aad.msal4j.labapi.LabServiceParameters.B2CIdentityProvider;
import com.microsoft.aad.msal4j.labapi.LabServiceParameters.FederationProvider;
import com.microsoft.aad.msal4j.labapi.LabServiceParameters.SignInAudience;
import com.microsoft.aad.msal4j.labapi.LabServiceParameters.UserType;

class UserQueryHelper {
    private UserType userType;
    private B2CIdentityProvider b2cIdentityProvider;
    private FederationProvider federationProvider;
    private SignInAudience signInAudience;
    private String azureEnvironment;

    UserType getUserType() {
        return userType;
    }

    void setUserType(UserType userType) {
        this.userType = userType;
    }

    B2CIdentityProvider getB2cIdentityProvider() {
        return b2cIdentityProvider;
    }

    void setB2cIdentityProvider(B2CIdentityProvider b2cIdentityProvider) {
        this.b2cIdentityProvider = b2cIdentityProvider;
    }

    FederationProvider getFederationProvider() {
        return federationProvider;
    }

    void setFederationProvider(FederationProvider federationProvider) {
        this.federationProvider = federationProvider;
    }

    String getAzureEnvironment() {
        return azureEnvironment;
    }

    void setAzureEnvironment(String azureEnvironment) {
        this.azureEnvironment = azureEnvironment;
    }

    SignInAudience getSignInAudience() {
        return signInAudience;
    }

    void setSignInAudience(SignInAudience signInAudience) {
        this.signInAudience = signInAudience;
    }

    static UserQueryHelper arlingtonUserQuery() {
        UserQueryHelper query = new UserQueryHelper();
        query.setUserType(UserType.CLOUD);
        query.setAzureEnvironment(AzureEnvironment.AZURE_US_GOVERNMENT);
        return query;
    }

    /**
     * Gets a B2C local account (username/password) from the lab.
     */
    static UserQueryHelper b2cLocalAccountQuery() {
        UserQueryHelper query = new UserQueryHelper();
        query.setUserType(UserType.B2C);
        query.setB2cIdentityProvider(B2CIdentityProvider.LOCAL);
        return query;
    }

    /**
     * Gets a CIAM user with Custom User Domain (CUD).
     * Example: login.customdomain.com instead of tenant.ciamlogin.com
     */
    static UserQueryHelper ciamCudUserQuery() {
        UserQueryHelper query = new UserQueryHelper();
        query.setFederationProvider(FederationProvider.CIAMCUD);
        query.setSignInAudience(SignInAudience.AzureAdMyOrg);
        return query;
    }

    /**
     * Gets a regular Microsoft Account (MSA) user, not tied to B2C.
     * This is for consumer accounts like outlook.com, hotmail.com, etc.
     */
    static UserQueryHelper msaUserQuery() {
        UserQueryHelper query = new UserQueryHelper();
        query.setUserType(UserType.MSA);
        return query;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (userType != null ? userType.hashCode() : 0);
        result = 31 * result + (azureEnvironment != null ? azureEnvironment.hashCode() : 0);
        result = 31 * result + (b2cIdentityProvider != null ? b2cIdentityProvider.hashCode() : 0);
        result = 31 * result + (federationProvider != null ? federationProvider.hashCode() : 0);
        result = 31 * result + (signInAudience != null ? signInAudience.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UserQueryHelper other = (UserQueryHelper) obj;
        return java.util.Objects.equals(userType, other.userType) &&
                java.util.Objects.equals(azureEnvironment, other.azureEnvironment) &&
                java.util.Objects.equals(b2cIdentityProvider, other.b2cIdentityProvider) &&
                java.util.Objects.equals(federationProvider, other.federationProvider) &&
                java.util.Objects.equals(signInAudience, other.signInAudience);
    }

    //Formats the fields as they would be seen in the query parameters section of a URL
    @Override
    public String toString() {
        StringBuilder queryString = new StringBuilder();

        if (userType != null) {
            queryString.append("userType=").append(userType).append("&");
        }

        if (b2cIdentityProvider != null) {
            queryString.append("b2cIdentityProvider=").append(b2cIdentityProvider).append("&");
        }

        if (federationProvider != null) {
            queryString.append("federationProvider=").append(federationProvider).append("&");
        }

        if (signInAudience != null) {
            queryString.append("signInAudience=").append(signInAudience).append("&");
        }

        if (azureEnvironment != null) {
            queryString.append("azureEnvironment=").append(azureEnvironment).append("&");
        }

        // Remove trailing "&" if any parameters were added
        if (queryString.length() > 0) {
            queryString.setLength(queryString.length() - 1);
        }

        return "?" + queryString;
    }
}