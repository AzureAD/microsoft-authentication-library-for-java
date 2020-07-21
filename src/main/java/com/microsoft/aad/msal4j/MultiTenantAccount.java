package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class MultiTenantAccount extends Account implements IMultiTenantAccount {

    private Map<String, ITenantProfile> tenantProfiles;

    public MultiTenantAccount(String homeAccountId, String environment, String username, Map<String, ?> idTokenClaims) {
        super(homeAccountId, environment, username, idTokenClaims);
    }

    @Override
    public Map<String, ITenantProfile> getTenantProfiles() {
        return tenantProfiles;
    }
}
