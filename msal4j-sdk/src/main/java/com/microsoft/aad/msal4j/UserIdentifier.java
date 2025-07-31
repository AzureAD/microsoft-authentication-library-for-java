// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Used for populating the X-AnchorMailbox header, which is used in the cached credential service
 * (CCS) routing
 */
public class UserIdentifier {

    // Format is "userObjectId@userTenantId"
    private static final String OID_HEADER_FORMAT = "%s@%s";

    private String upn;
    private String oid;

    private UserIdentifier() {
    }

    /**
     * TODO: Add description
     */
    public static UserIdentifier fromUpn(String upn) {
        UserIdentifier userIdentifier = new UserIdentifier();
        userIdentifier.upn = upn;
        return userIdentifier;
    }

    /**
     * TODO: Add description
     */
    public static UserIdentifier fromHomeAccountId(String homeAccountId) {
        UserIdentifier userIdentifier = new UserIdentifier();

        // HomeAccountId is userObjectId.userTenantId
        String[] homeAccountIdParts = homeAccountId.split("\\.");
        if (homeAccountIdParts.length < 2
                || StringHelper.isBlank(homeAccountIdParts[0])
                || StringHelper.isBlank(homeAccountIdParts[1])) {
            userIdentifier.oid = null;
            return userIdentifier;
        }

        userIdentifier.oid = String.format(OID_HEADER_FORMAT, homeAccountIdParts[0], homeAccountIdParts[1]);
        return userIdentifier;
    }

    String upn() {
        return this.upn;
    }

    String oid() {
        return this.oid;
    }
}
