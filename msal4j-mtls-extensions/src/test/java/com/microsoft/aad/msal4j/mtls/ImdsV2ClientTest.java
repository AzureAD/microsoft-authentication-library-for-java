// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.microsoft.aad.msal4j.ManagedIdentityMtlsHttpResponse;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ImdsV2ClientTest {

    @Test
    void metadataUsesCurrentV2ContractAndIdentitySelector() {
        AtomicReference<String> url = new AtomicReference<>();
        ManagedIdentityMtlsRequest request = request("client_id", "client id", http -> {
            url.set(http.url());
            return response("{\"clientId\":\"client id\",\"tenantId\":\"tenant\","
                    + "\"cuId\":{\"vmId\":\"vm\",\"vmssId\":\"set\"},"
                    + "\"attestationEndpoint\":\"https://maa.example\"}");
        });

        ImdsV2Client.PlatformMetadata metadata =
                ImdsV2Client.getPlatformMetadata(request);

        assertTrue(url.get().contains("/metadata/identity/getplatformmetadata"));
        assertTrue(url.get().contains("cred-api-version=2.0"));
        assertTrue(url.get().contains("client_id=client+id"));
        assertEquals("client id", metadata.clientId);
        assertEquals("vm", metadata.cuId());
    }

    @Test
    void issueCredentialRequiresAttestationAndCurrentFields() {
        ManagedIdentityMtlsRequest request = request(null, null, http -> {
            assertEquals("POST", http.method());
            assertTrue(http.body().contains("\"attestation_token\":\"jwt\""));
            return response("{\"certificate\":\"cert\","
                    + "\"mtls_authentication_endpoint\":\"https://login.example/token\","
                    + "\"client_id\":\"client\",\"tenant_id\":\"tenant\","
                    + "\"identity_type\":\"SystemAssigned\"}");
        });

        assertThrows(MtlsMsiException.class,
                () -> ImdsV2Client.issueCredential(request, "csr", ""));
        ImdsV2Client.CredentialResponse credential =
                ImdsV2Client.issueCredential(request, "csr", "jwt");

        assertEquals("client", credential.clientId);
        assertEquals("https://login.example/token",
                credential.mtlsAuthenticationEndpoint);
    }

    @Test
    void issueCredentialOmitsAttestationWhenNotRequested() {
        ManagedIdentityMtlsRequest request = request(
                null,
                null,
                http -> {
                    assertFalse(http.body().contains("attestation_token"));
                    return response("{\"certificate\":\"cert\","
                            + "\"mtls_authentication_endpoint\":\"https://login.example/token\","
                            + "\"client_id\":\"client\",\"tenant_id\":\"tenant\","
                            + "\"identity_type\":\"SystemAssigned\"}");
                },
                false);

        assertDoesNotThrow(
                () -> ImdsV2Client.issueCredential(request, "csr", null));
    }

    @Test
    void incompleteOrFailedImdsResponseFailsClosed() {
        assertThrows(MtlsMsiException.class,
                () -> ImdsV2Client.getPlatformMetadata(
                        request(null, null, http -> response("{}"))));
        assertThrows(MtlsMsiException.class,
                () -> ImdsV2Client.getPlatformMetadata(
                        request(null, null, http ->
                                new ManagedIdentityMtlsHttpResponse(
                                        500, "failure", Collections.emptyMap()))));
    }

    @Test
    void metadataRejectsResponsesWithoutImdsServerMarker() {
        assertThrows(MtlsMsiException.class,
                () -> ImdsV2Client.getPlatformMetadata(
                        request(null, null, http ->
                                new ManagedIdentityMtlsHttpResponse(
                                        200,
                                        "{\"clientId\":\"client\",\"tenantId\":\"tenant\","
                                                + "\"cuId\":{\"vmId\":\"vm\"},"
                                                + "\"attestationEndpoint\":\"https://maa.example\"}",
                                        Collections.emptyMap()))));
    }

    private static ManagedIdentityMtlsRequest request(
            String selector,
            String value,
            com.microsoft.aad.msal4j.IManagedIdentityMtlsHttpClient client) {
        return request(selector, value, client, true);
    }

    private static ManagedIdentityMtlsRequest request(
            String selector,
            String value,
            com.microsoft.aad.msal4j.IManagedIdentityMtlsHttpClient client,
            boolean attestationEnabled) {
        return new ManagedIdentityMtlsRequest(
                selector, value, "binding", "correlation", client,
                attestationEnabled);
    }

    private static ManagedIdentityMtlsHttpResponse response(String body) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Server", Collections.singletonList("IMDS/150.0"));
        return new ManagedIdentityMtlsHttpResponse(
                200, body, headers);
    }
}
