package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

@Accessors(fluent = true)
@Getter
@Setter
public class TenantProfile extends Account implements ITenantProfile{

    String tenantId;

    public TenantProfile(String homeAccountId, String environment, String username, Map<String, ?> idTokenClaims, String tenantId) {
        super(homeAccountId, environment, username, idTokenClaims);
        this.tenantId = tenantId;
    }
}
