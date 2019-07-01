//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package labapi;

import java.util.Set;

public class UserQuery {

    private final boolean isMamUser;
    private final boolean isMfaUser;
    private final boolean isFederatedUser;
    private final boolean isExternalUser;
    private final boolean useBetaEndpoint;
    private final Set<String> licenses;
    private final UserType userType;
    private final FederationProvider federationProvider;
    private final B2CIdentityProvider b2CIdentityProvider;
    private final NationalCloud nationalCloud;

    public static class Builder {
        private boolean isMamUser = false;
        private boolean isMfaUser = false;
        private boolean isFederatedUser = false;
        private boolean isExternalUser = false;
        private boolean useBetaEndpoint = false;
        private Set<String> licenses;
        private UserType userType;
        private FederationProvider federationProvider;
        private B2CIdentityProvider b2CIdentityProvider;
        private NationalCloud nationalCloud;

        public Builder isMamUser(boolean val){
            isMamUser = val;
            return this;
        }

        public Builder isMfaUser(boolean val){
            isMfaUser = val;
            return this;
        }

        public Builder isFederatedUser(boolean val){
            isFederatedUser = val;
            return this;
        }

        public Builder isExternalUser(boolean val){
            isExternalUser = val;
            return this;
        }
        public Builder useBetaEnpoint(boolean val){
            useBetaEndpoint = val;
            return this;
        }

        public Builder licenses(Set<String> val){
            licenses = val;
            return this;
        }

        public Builder userType(UserType val){
            userType = val;
            return this;
        }

        public Builder federationProvider(FederationProvider val){
            federationProvider = val;
            return this;
        }

        public Builder b2CIdentityProvider(B2CIdentityProvider val){
            b2CIdentityProvider = val;
            return this;
        }

        public Builder nationalCloud(NationalCloud val){
            nationalCloud = val;
            return this;
        }

        public UserQuery build(){
            return new UserQuery(this);
        }
    }

    private UserQuery(Builder builder){
        this.isMamUser = builder.isMamUser;
        this.isMfaUser = builder.isMfaUser;
        this.isFederatedUser = builder.isFederatedUser;
        this.isExternalUser = builder.isExternalUser;
        this.useBetaEndpoint = builder.useBetaEndpoint;
        this.licenses = builder.licenses;
        this.userType = builder.userType;
        this.federationProvider = builder.federationProvider;
        this.b2CIdentityProvider = builder.b2CIdentityProvider;
        this.nationalCloud = builder.nationalCloud;
    }

    public FederationProvider getFederationProvider() {
        return federationProvider;
    }

    public boolean isMamUser() {
        return isMamUser;
    }

    public boolean isMfaUser() {
        return isMfaUser;
    }

    public Set<String> getLicenses() {
        return licenses;
    }

    public boolean isFederatedUser() {
        return isFederatedUser;
    }

    public UserType getUserType() {
        return userType;
    }

    public boolean isExternalUser() {
        return isExternalUser;
    }

    public B2CIdentityProvider getB2CIdentityProvider() {
        return b2CIdentityProvider;
    }

    public boolean useBetaEndpoint(){
        return useBetaEndpoint;
    }

    public NationalCloud getNationalCloud(){
        return nationalCloud;
    }
}
