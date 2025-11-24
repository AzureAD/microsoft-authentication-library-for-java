// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

import com.microsoft.aad.msal4j.labapi2.LabServiceParameters.B2CIdentityProvider;
import com.microsoft.aad.msal4j.labapi2.LabServiceParameters.FederationProvider;
import com.microsoft.aad.msal4j.labapi2.LabServiceParameters.SignInAudience;
import com.microsoft.aad.msal4j.labapi2.LabServiceParameters.UserType;

class UserQuery {
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

    static UserQuery arlingtonUserQuery() {
        UserQuery query = new UserQuery();
        query.setUserType(UserType.CLOUD);
        query.setAzureEnvironment(AzureEnvironment.AZURE_US_GOVERNMENT);
        return query;
    }

    /**
     * Gets a B2C local account (username/password) from the lab.
     */
    static UserQuery b2cLocalAccountQuery() {
        UserQuery query = new UserQuery();
        query.setUserType(UserType.B2C);
        query.setB2cIdentityProvider(B2CIdentityProvider.LOCAL);
        return query;
    }

    /**
     * Gets a CIAM user with Custom User Domain (CUD).
     * Example: login.customdomain.com instead of tenant.ciamlogin.com
     */
    static UserQuery ciamCudUserQuery() {
        UserQuery query = new UserQuery();
        query.setFederationProvider(FederationProvider.CIAMCUD);
        query.setSignInAudience(SignInAudience.AzureAdMyOrg);
        return query;
    }

    /**
     * Gets a regular Microsoft Account (MSA) user, not tied to B2C.
     * This is for consumer accounts like outlook.com, hotmail.com, etc.
     */
    static UserQuery msaUserQuery() {
        UserQuery query = new UserQuery();
        query.setUserType(UserType.MSA);
        return query;
    }

    @Override
    public int hashCode() {
        // Implement proper hashCode for caching
        int result = 17;
        result = 31 * result + (userType != null ? userType.hashCode() : 0);
        result = 31 * result + (azureEnvironment != null ? azureEnvironment.hashCode() : 0);
        result = 31 * result + (b2cIdentityProvider != null ? b2cIdentityProvider.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UserQuery other = (UserQuery) obj;
        return java.util.Objects.equals(userType, other.userType) &&
                java.util.Objects.equals(azureEnvironment, other.azureEnvironment) &&
                java.util.Objects.equals(b2cIdentityProvider, other.b2cIdentityProvider);
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