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

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import java.util.UUID;

public class LabUser {

    @SerializedName("objectId")
    private UUID ObjectId;

    @SerializedName("userType")
    private UserType userType;

    @SerializedName("upn")
    private String upn;

    @SerializedName("credentialVaultKeyName")
    private String credentialUrl;

    @SerializedName("external")
    private String isExternal;

    @SerializedName("mfa")
    private boolean isMfa;

    @SerializedName("mam")
    private boolean isMam;

    @SerializedName("licenses")
    private Set<String> licenses;

    @SerializedName("isFederated")
    private boolean isFederated;

    @SerializedName("federationProvider")
    private FederationProvider federationProvider;

    @SerializedName("tenantId")
    private String tenantId;

    @SerializedName("hometenantId")
    private String homeTenantId;

    @SerializedName("homeUPN")
    private String homeUpn;

    @SerializedName("B2CProvider")
    private B2CIdentityProvider b2CIdentityProvider;

    private LabUser homeUser;
    private String password = null;

    public UUID getObjectId() {
        return ObjectId;
    }

    public void setObjectId(UUID objectId) {
        ObjectId = objectId;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public String getUpn() {
        return upn;
    }

    public void setUpn(String upn) {
        this.upn = upn;
    }

    public String getCredentialUrl() {
        return credentialUrl;
    }

    public void setCredentialUrl(String credentialUrl) {
        this.credentialUrl = credentialUrl;
    }

    public LabUser getHomeUser() {
        return homeUser;
    }

    public void setHomeUser(LabUser homeUser) {
        this.homeUser = homeUser;
    }

    public boolean isMfa() {
        return isMfa;
    }

    public void setMfa(boolean mfa) {
        isMfa = mfa;
    }

    public boolean isMam() {
        return isMam;
    }

    public void setMam(boolean mam) {
        isMam = mam;
    }

    public Set<String> getLicenses() {
        return licenses;
    }

    public void setLicenses(Set<String> licenses) {
        this.licenses = licenses;
    }

    public boolean isFederated() {
        return isFederated;
    }

    public void setFederated(boolean federated) {
        isFederated = federated;
    }

    public FederationProvider getFederationProvider() {
        return federationProvider;
    }

    public void setFederationProvider(FederationProvider federationProvider) {
        this.federationProvider = federationProvider;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getHomeTenantId() {
        return homeTenantId;
    }

    public void setHomeTenantId(String homeTenantId) {
        this.homeTenantId = homeTenantId;
    }

    public String getHomeUpn() {
        return homeUpn;
    }

    public void setHomeUpn(String homeUpn) {
        this.homeUpn = homeUpn;
    }

    public B2CIdentityProvider getB2CIdentityProvider() {
        return b2CIdentityProvider;
    }

    public void setB2CIdentityProvider(B2CIdentityProvider b2CIdentityProvider) {
        this.b2CIdentityProvider = b2CIdentityProvider;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIsExternal() {
        return isExternal;
    }

    public void setIsExternal(String isExternal) {
        this.isExternal = isExternal;
    }

    public void initializeHomeUser(){
        homeUser = new LabUser();

        homeUser.ObjectId = ObjectId;
        homeUser.userType = userType;
        homeUser.credentialUrl = credentialUrl;
        homeUser.homeUser = homeUser;
        homeUser.isExternal = isExternal;
        homeUser.isMfa = isMfa;
        homeUser.isMam = isMam;
        homeUser.licenses = licenses;
        homeUser.isFederated = isFederated;
        homeUser.federationProvider = federationProvider;
        homeUser.homeTenantId = homeTenantId;
        homeUser.homeUpn = homeUpn;
        homeUser.tenantId = homeTenantId;
        homeUser.upn = homeUpn;
        homeUser.b2CIdentityProvider = b2CIdentityProvider;
    }
}
