// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

import com.microsoft.aad.msal4j.labapi2.LabServiceParameters.*;

public class UserQuery {
    private UserType userType;
    private B2CIdentityProvider b2cIdentityProvider;
    private FederationProvider federationProvider;
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

    public static UserQuery b2cLocalAccountUserQuery() {
        UserQuery query = new UserQuery();
        query.setUserType(UserType.B2C);
        query.setB2cIdentityProvider(B2CIdentityProvider.LOCAL);
        return query;
    }

    public static UserQuery arlingtonUserQuery() {
        UserQuery query = new UserQuery();
        query.setUserType(UserType.CLOUD);
        query.setAzureEnvironment(AzureEnvironment.AZURE_US_GOVERNMENT);
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