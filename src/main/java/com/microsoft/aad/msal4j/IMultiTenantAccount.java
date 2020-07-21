package com.microsoft.aad.msal4j;

import java.util.Map;

public interface IMultiTenantAccount extends IAccount {

    Map<String, ITenantProfile> getTenantProfiles();
}
