// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class WsTrustFederationTest {

    // ========== Credential ==========

    @Test
    void credential_fromJson_allFields() throws IOException {
        String json = "{"
                + "\"home_account_id\":\"uid.utid\","
                + "\"environment\":\"login.microsoftonline.com\","
                + "\"client_id\":\"client-123\","
                + "\"secret\":\"secret-token\","
                + "\"user_assertion_hash\":\"hash-abc\""
                + "}";

        Credential cred = parseJson(json, Credential::fromJson);

        assertEquals("uid.utid", cred.homeAccountId());
        assertEquals("login.microsoftonline.com", cred.environment());
        assertEquals("client-123", cred.clientId());
        assertEquals("secret-token", cred.secret());
        assertEquals("hash-abc", cred.userAssertionHash());
    }

    @Test
    void credential_fromJson_partialFields() throws IOException {
        String json = "{\"home_account_id\":\"uid\",\"environment\":\"env\"}";

        Credential cred = parseJson(json, Credential::fromJson);

        assertEquals("uid", cred.homeAccountId());
        assertEquals("env", cred.environment());
        assertNull(cred.clientId());
        assertNull(cred.secret());
        assertNull(cred.userAssertionHash());
    }

    @Test
    void credential_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"home_account_id\":\"uid\",\"extra\":123}";

        Credential cred = parseJson(json, Credential::fromJson);

        assertEquals("uid", cred.homeAccountId());
    }

    @Test
    void credential_settersAndGetters() {
        Credential cred = new Credential();

        cred.homeAccountId("home-1");
        cred.environment("env-1");
        cred.clientId("client-1");
        cred.secret("secret-1");
        cred.userAssertionHash("hash-1");

        assertEquals("home-1", cred.homeAccountId());
        assertEquals("env-1", cred.environment());
        assertEquals("client-1", cred.clientId());
        assertEquals("secret-1", cred.secret());
        assertEquals("hash-1", cred.userAssertionHash());
    }

    @Test
    void credential_toJson_allFields() throws IOException {
        Credential cred = new Credential();
        cred.homeAccountId("uid.utid");
        cred.environment("login.microsoftonline.com");
        cred.clientId("cid");
        cred.secret("sec");
        cred.userAssertionHash("hash");

        String output = writeToJson(cred);

        assertTrue(output.contains("\"home_account_id\":\"uid.utid\""));
        assertTrue(output.contains("\"environment\":\"login.microsoftonline.com\""));
        assertTrue(output.contains("\"client_id\":\"cid\""));
        assertTrue(output.contains("\"secret\":\"sec\""));
        assertTrue(output.contains("\"user_assertion_hash\":\"hash\""));
    }

    @Test
    void credential_toJson_roundTrip() throws IOException {
        String json = "{"
                + "\"home_account_id\":\"uid\","
                + "\"environment\":\"env\","
                + "\"client_id\":\"cid\","
                + "\"secret\":\"sec\","
                + "\"user_assertion_hash\":\"hash\""
                + "}";

        Credential original = parseJson(json, Credential::fromJson);
        String serialized = writeToJson(original);
        Credential roundTripped = parseJson(serialized, Credential::fromJson);

        assertEquals(original.homeAccountId(), roundTripped.homeAccountId());
        assertEquals(original.environment(), roundTripped.environment());
        assertEquals(original.clientId(), roundTripped.clientId());
        assertEquals(original.secret(), roundTripped.secret());
        assertEquals(original.userAssertionHash(), roundTripped.userAssertionHash());
    }

    // ========== BindingPolicy ==========

    @Test
    void bindingPolicy_singleArgConstructor() {
        BindingPolicy policy = new BindingPolicy("policy-value");

        assertEquals("policy-value", policy.getValue());
        assertNull(policy.getUrl());
        assertNull(policy.getVersion());
    }

    @Test
    void bindingPolicy_twoArgConstructor() {
        BindingPolicy policy = new BindingPolicy("https://adfs.example.com/trust", WSTrustVersion.WSTRUST13);

        assertEquals("https://adfs.example.com/trust", policy.getUrl());
        assertEquals(WSTrustVersion.WSTRUST13, policy.getVersion());
        assertNull(policy.getValue());
    }

    @Test
    void bindingPolicy_settersAndGetters() {
        BindingPolicy policy = new BindingPolicy("initial");

        policy.setValue("updated-value");
        policy.setUrl("https://new-url");
        policy.setVersion(WSTrustVersion.WSTRUST2005);

        assertEquals("updated-value", policy.getValue());
        assertEquals("https://new-url", policy.getUrl());
        assertEquals(WSTrustVersion.WSTRUST2005, policy.getVersion());
    }

    @Test
    void bindingPolicy_versionUndefined() {
        BindingPolicy policy = new BindingPolicy("https://url", WSTrustVersion.UNDEFINED);

        assertEquals(WSTrustVersion.UNDEFINED, policy.getVersion());
    }

    // ========== IntegratedWindowsAuthorizationGrant ==========

    @Test
    void integratedWindowsAuthGrant_constructor() {
        IntegratedWindowsAuthorizationGrant grant = new IntegratedWindowsAuthorizationGrant(
                Collections.singleton("openid"), "user@domain.com", null);

        assertEquals("user@domain.com", grant.getUserName());
        assertNull(grant.toParameters());
    }

    @Test
    void integratedWindowsAuthGrant_withClaims() {
        ClaimsRequest claims = new ClaimsRequest();
        IntegratedWindowsAuthorizationGrant grant = new IntegratedWindowsAuthorizationGrant(
                Collections.singleton("profile"), "admin@corp.net", claims);

        assertEquals("admin@corp.net", grant.getUserName());
    }

    // ========== IdToken ==========

    @Test
    void idToken_fromJson_allFields() throws IOException {
        String json = "{"
                + "\"iss\":\"https://login.microsoftonline.com/tenant/v2.0\","
                + "\"sub\":\"sub-123\","
                + "\"aud\":\"client-id\","
                + "\"exp\":1717430400,"
                + "\"iat\":1717426800,"
                + "\"nbf\":1717426800,"
                + "\"name\":\"Test User\","
                + "\"preferred_username\":\"testuser@example.com\","
                + "\"oid\":\"oid-456\","
                + "\"tid\":\"tid-789\","
                + "\"upn\":\"testuser@example.com\","
                + "\"unique_name\":\"testuser\""
                + "}";

        IdToken idToken = parseJson(json, IdToken::fromJson);

        assertEquals("https://login.microsoftonline.com/tenant/v2.0", idToken.issuer);
        assertEquals("sub-123", idToken.subject);
        assertEquals("client-id", idToken.audience);
        assertEquals(1717430400L, idToken.expirationTime.longValue());
        assertEquals(1717426800L, idToken.issuedAt.longValue());
        assertEquals(1717426800L, idToken.notBefore.longValue());
        assertEquals("Test User", idToken.name);
        assertEquals("testuser@example.com", idToken.preferredUsername);
        assertEquals("oid-456", idToken.objectIdentifier);
        assertEquals("tid-789", idToken.tenantIdentifier);
        assertEquals("testuser@example.com", idToken.upn);
        assertEquals("testuser", idToken.uniqueName);
    }

    @Test
    void idToken_fromJson_partialFields() throws IOException {
        String json = "{\"iss\":\"issuer\",\"sub\":\"subject\",\"aud\":\"audience\"}";

        IdToken idToken = parseJson(json, IdToken::fromJson);

        assertEquals("issuer", idToken.issuer);
        assertEquals("subject", idToken.subject);
        assertEquals("audience", idToken.audience);
        assertNull(idToken.expirationTime);
        assertNull(idToken.issuedAt);
        assertNull(idToken.notBefore);
        assertNull(idToken.name);
        assertNull(idToken.preferredUsername);
        assertNull(idToken.objectIdentifier);
        assertNull(idToken.tenantIdentifier);
        assertNull(idToken.upn);
        assertNull(idToken.uniqueName);
    }

    @Test
    void idToken_fromJson_unknownFieldsSkipped() throws IOException {
        String json = "{\"iss\":\"issuer\",\"nonce\":\"abc\",\"email\":\"user@test.com\"}";

        IdToken idToken = parseJson(json, IdToken::fromJson);

        assertEquals("issuer", idToken.issuer);
    }

    @Test
    void idToken_toJson_allFields() throws IOException {
        IdToken idToken = new IdToken();
        idToken.issuer = "iss";
        idToken.subject = "sub";
        idToken.audience = "aud";
        idToken.expirationTime = 99999L;
        idToken.issuedAt = 11111L;
        idToken.notBefore = 11111L;
        idToken.name = "User Name";
        idToken.preferredUsername = "user@example.com";
        idToken.objectIdentifier = "oid";
        idToken.tenantIdentifier = "tid";
        idToken.upn = "user@example.com";
        idToken.uniqueName = "unique";

        String output = writeToJson(idToken);

        assertTrue(output.contains("\"iss\":\"iss\""));
        assertTrue(output.contains("\"sub\":\"sub\""));
        assertTrue(output.contains("\"aud\":\"aud\""));
        assertTrue(output.contains("\"name\":\"User Name\""));
        assertTrue(output.contains("\"preferred_username\":\"user@example.com\""));
        assertTrue(output.contains("\"oid\":\"oid\""));
        assertTrue(output.contains("\"tid\":\"tid\""));
        assertTrue(output.contains("\"upn\":\"user@example.com\""));
        assertTrue(output.contains("\"unique_name\":\"unique\""));
    }

    @Test
    void idToken_toJson_roundTrip() throws IOException {
        String json = "{"
                + "\"iss\":\"issuer\","
                + "\"sub\":\"subject\","
                + "\"aud\":\"audience\","
                + "\"exp\":1234567890,"
                + "\"iat\":1234567800,"
                + "\"nbf\":1234567800,"
                + "\"name\":\"Test\","
                + "\"preferred_username\":\"test@test.com\","
                + "\"oid\":\"oid-1\","
                + "\"tid\":\"tid-1\","
                + "\"upn\":\"upn@test.com\","
                + "\"unique_name\":\"unique-1\""
                + "}";

        IdToken original = parseJson(json, IdToken::fromJson);
        String serialized = writeToJson(original);
        IdToken roundTripped = parseJson(serialized, IdToken::fromJson);

        assertEquals(original.issuer, roundTripped.issuer);
        assertEquals(original.subject, roundTripped.subject);
        assertEquals(original.audience, roundTripped.audience);
        assertEquals(original.expirationTime, roundTripped.expirationTime);
        assertEquals(original.name, roundTripped.name);
        assertEquals(original.preferredUsername, roundTripped.preferredUsername);
        assertEquals(original.objectIdentifier, roundTripped.objectIdentifier);
        assertEquals(original.tenantIdentifier, roundTripped.tenantIdentifier);
        assertEquals(original.upn, roundTripped.upn);
        assertEquals(original.uniqueName, roundTripped.uniqueName);
    }

    // ========== WSTrustVersion ==========

    @Test
    void wsTrustVersion_pathValues() {
        assertFalse(WSTrustVersion.WSTRUST13.responseTokenTypePath().isEmpty());
        assertFalse(WSTrustVersion.WSTRUST13.responseSecurityTokenPath().isEmpty());

        assertFalse(WSTrustVersion.WSTRUST2005.responseTokenTypePath().isEmpty());
        assertFalse(WSTrustVersion.WSTRUST2005.responseSecurityTokenPath().isEmpty());

        assertTrue(WSTrustVersion.UNDEFINED.responseTokenTypePath().isEmpty());
        assertTrue(WSTrustVersion.UNDEFINED.responseSecurityTokenPath().isEmpty());
    }

    @Test
    void wsTrustVersion_wsTrust13_containsCorrectPaths() {
        assertTrue(WSTrustVersion.WSTRUST13.responseTokenTypePath()
                .contains("RequestSecurityTokenResponseCollection"));
        assertTrue(WSTrustVersion.WSTRUST13.responseSecurityTokenPath()
                .contains("RequestedSecurityToken"));
    }

    // ========== Helpers ==========

    @FunctionalInterface
    interface JsonParser<T> {
        T parse(JsonReader reader) throws IOException;
    }

    private <T> T parseJson(String json, JsonParser<T> parser) throws IOException {
        try (JsonReader reader = JsonProviders.createReader(new StringReader(json))) {
            return parser.parse(reader);
        }
    }

    private <T extends com.azure.json.JsonSerializable<T>> String writeToJson(T serializable)
            throws IOException {
        StringWriter sw = new StringWriter();
        try (com.azure.json.JsonWriter writer = JsonProviders.createWriter(sw)) {
            serializable.toJson(writer);
        }
        return sw.toString();
    }
}
