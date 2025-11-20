// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

import com.microsoft.aad.msal4j.labapi2.LabServiceParameters.*;

public class UserQuery {
    private UserType userType;
    private B2CIdentityProvider b2cIdentityProvider;
    private FederationProvider federationProvider;
    private SignInAudience signInAudience;
    private String azureEnvironment;

    // Getters and Setters
    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }

    public B2CIdentityProvider getB2cIdentityProvider() { return b2cIdentityProvider; }
    public void setB2cIdentityProvider(B2CIdentityProvider b2cIdentityProvider) {
        this.b2cIdentityProvider = b2cIdentityProvider;
    }

    public FederationProvider getFederationProvider() { return federationProvider; }
    public void setFederationProvider(FederationProvider federationProvider) {
        this.federationProvider = federationProvider;
    }

    public String getAzureEnvironment() { return azureEnvironment; }
    public void setAzureEnvironment(String azureEnvironment) {
        this.azureEnvironment = azureEnvironment;
    }

    public SignInAudience getSignInAudience() { return signInAudience; }
    public void setSignInAudience(SignInAudience signInAudience) {
        this.signInAudience = signInAudience;
    }

    public static UserQuery arlingtonUserQuery() {
        UserQuery query = new UserQuery();
        query.setUserType(UserType.CLOUD);
        query.setAzureEnvironment(AzureEnvironment.AZURE_US_GOVERNMENT);
        return query;
    }

    /**
     * Gets a B2C local account (username/password) from the lab.
     */
    public static UserQuery b2cLocalAccountQuery() {
        UserQuery query = new UserQuery();
        query.setUserType(UserType.B2C);
        query.setB2cIdentityProvider(B2CIdentityProvider.LOCAL);
        return query;
    }

    /**
     * Gets a B2C user that authenticates with Microsoft Account (MSA).
     */
    public static UserQuery b2cMsaAccountQuery() {
        UserQuery query = new UserQuery();
        query.setUserType(UserType.B2C);
        query.setB2cIdentityProvider(B2CIdentityProvider.MSA);
        return query;
    }

    /**
     * Gets a CIAM user with standard domain (tenant.ciamlogin.com).
     * Uses the Lab API to query for CIAM users.
     */
    public static UserQuery ciamUserQuery() {
        UserQuery query = new UserQuery();
        query.setFederationProvider(FederationProvider.CIAM);
        query.setSignInAudience(SignInAudience.AzureAdMyOrg);
        return query;
    }

    /**
     * Gets a CIAM user with Custom User Domain (CUD).
     * Example: login.customdomain.com instead of tenant.ciamlogin.com
     */
    public static UserQuery ciamCudUserQuery() {
        UserQuery query = new UserQuery();
        query.setFederationProvider(FederationProvider.CIAMCUD);
        query.setSignInAudience(SignInAudience.AzureAdMyOrg);
        return query;
    }

    /**
     * Gets a regular Microsoft Account (MSA) user, not tied to B2C.
     * This is for consumer accounts like outlook.com, hotmail.com, etc.
     */
    public static UserQuery msaUserQuery() {
        UserQuery query = new UserQuery();
        query.setUserType(UserType.MSA);
        return query;
    }

    /**
     * Gets a CIAM user for OBO scenarios.
     * CIAM supports OBO flows, so this query gets a CIAM user that can be used
     * in OBO tests.
     */
    public static UserQuery ciamOboUserQuery() {
        UserQuery query = new UserQuery();
        query.setFederationProvider(FederationProvider.CIAM);
        query.setSignInAudience(SignInAudience.AzureAdMyOrg);
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
}