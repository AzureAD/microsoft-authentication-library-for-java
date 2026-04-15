// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link AgentIdentity} model class (§9 from AgentIDs_ComponentsReference).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgentIdentityTest {

    private static final String AGENT_APP_ID = "agent-app-id-123";
    private static final String USER_UPN = "alice@contoso.com";
    private static final UUID USER_OID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    // ========================================================================
    // Constructor: by Object ID (recommended)
    // ========================================================================

    @Test
    void constructor_withOid_setsPropertiesCorrectly() {
        AgentIdentity identity = new AgentIdentity(AGENT_APP_ID, USER_OID);

        assertEquals(AGENT_APP_ID, identity.agentApplicationId());
        assertEquals(USER_OID, identity.userObjectId());
        assertNull(identity.username());
        assertTrue(identity.hasUserIdentifier());
    }

    @Test
    void constructor_withOid_nullAgentAppId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new AgentIdentity(null, USER_OID));
    }

    @Test
    void constructor_withOid_emptyAgentAppId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new AgentIdentity("", USER_OID));
    }

    @Test
    void constructor_withOid_nullOid_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new AgentIdentity(AGENT_APP_ID, null));
    }

    // ========================================================================
    // Factory: withUsername (by UPN)
    // ========================================================================

    @Test
    void withUsername_setsPropertiesCorrectly() {
        AgentIdentity identity = AgentIdentity.withUsername(AGENT_APP_ID, USER_UPN);

        assertEquals(AGENT_APP_ID, identity.agentApplicationId());
        assertNull(identity.userObjectId());
        assertEquals(USER_UPN, identity.username());
        assertTrue(identity.hasUserIdentifier());
    }

    @Test
    void withUsername_nullAgentAppId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                AgentIdentity.withUsername(null, USER_UPN));
    }

    @Test
    void withUsername_emptyAgentAppId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                AgentIdentity.withUsername("", USER_UPN));
    }

    @Test
    void withUsername_nullUsername_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                AgentIdentity.withUsername(AGENT_APP_ID, null));
    }

    @Test
    void withUsername_emptyUsername_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                AgentIdentity.withUsername(AGENT_APP_ID, ""));
    }

    // ========================================================================
    // Factory: appOnly (no user)
    // ========================================================================

    @Test
    void appOnly_setsPropertiesCorrectly() {
        AgentIdentity identity = AgentIdentity.appOnly(AGENT_APP_ID);

        assertEquals(AGENT_APP_ID, identity.agentApplicationId());
        assertNull(identity.userObjectId());
        assertNull(identity.username());
        assertFalse(identity.hasUserIdentifier());
    }

    @Test
    void appOnly_nullAgentAppId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                AgentIdentity.appOnly(null));
    }

    @Test
    void appOnly_emptyAgentAppId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                AgentIdentity.appOnly(""));
    }

    // ========================================================================
    // hasUserIdentifier behavior
    // ========================================================================

    @Test
    void hasUserIdentifier_withOid_returnsTrue() {
        AgentIdentity identity = new AgentIdentity(AGENT_APP_ID, USER_OID);
        assertTrue(identity.hasUserIdentifier());
    }

    @Test
    void hasUserIdentifier_withUsername_returnsTrue() {
        AgentIdentity identity = AgentIdentity.withUsername(AGENT_APP_ID, USER_UPN);
        assertTrue(identity.hasUserIdentifier());
    }

    @Test
    void hasUserIdentifier_appOnly_returnsFalse() {
        AgentIdentity identity = AgentIdentity.appOnly(AGENT_APP_ID);
        assertFalse(identity.hasUserIdentifier());
    }
}
