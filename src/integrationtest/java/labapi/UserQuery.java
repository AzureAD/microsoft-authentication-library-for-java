//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
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
