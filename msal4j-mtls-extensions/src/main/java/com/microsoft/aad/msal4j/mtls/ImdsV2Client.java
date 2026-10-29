// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsHttpRequest;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsHttpResponse;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsRequest;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ImdsV2Client {

    private static final String IMDS_BASE = "http://169.254.169.254";
    private static final String PLATFORM_METADATA_PATH =
            "/metadata/identity/getplatformmetadata";
    private static final String ISSUE_CREDENTIAL_PATH =
            "/metadata/identity/issuecredential";
    private static final String API_VERSION_QUERY = "cred-api-version=2.0";

    private ImdsV2Client() {
    }

    static PlatformMetadata getPlatformMetadata(ManagedIdentityMtlsRequest request) {
        ManagedIdentityMtlsHttpResponse httpResponse = execute(
                request,
                "GET",
                buildUrl(PLATFORM_METADATA_PATH, request),
                null);
        validateImdsOrigin(httpResponse);
        Map<String, Object> response = parseResponse(httpResponse.body());
        Object identity = response.get("cuId");
        Map<?, ?> identityFields = identity instanceof Map ? (Map<?, ?>) identity : null;
        PlatformMetadata metadata = new PlatformMetadata(
                stringField(response, "clientId"),
                stringField(response, "tenantId"),
                stringField(identityFields, "vmId"),
                stringField(identityFields, "vmssId"),
                stringField(response, "attestationEndpoint"));
        if (isBlank(metadata.clientId)
                || isBlank(metadata.tenantId)
                || isBlank(metadata.cuId())
                || (request.attestationEnabled()
                    && isBlank(metadata.attestationEndpoint))) {
            throw new MtlsMsiException(
                    "IMDS getplatformmetadata returned an incomplete attested KeyGuard contract.");
        }
        return metadata;
    }

    static CredentialResponse issueCredential(
            ManagedIdentityMtlsRequest request,
            String csr,
            String attestationToken) {
        if (request.attestationEnabled() && isBlank(attestationToken)) {
            throw new MtlsMsiException(
                    "KeyGuard attestation failed; no attestation token was produced.");
        }
        String body = "{\"csr\":\"" + escapeJson(csr) + "\""
                + (isBlank(attestationToken)
                    ? ""
                    : ",\"attestation_token\":\""
                        + escapeJson(attestationToken) + "\"")
                + "}";
        ManagedIdentityMtlsHttpResponse httpResponse = execute(
                request,
                "POST",
                buildUrl(ISSUE_CREDENTIAL_PATH, request),
                body);
        validateImdsOrigin(httpResponse);
        Map<String, Object> response = parseResponse(httpResponse.body());
        CredentialResponse credential = new CredentialResponse(
                stringField(response, "certificate"),
                stringField(response, "mtls_authentication_endpoint"),
                stringField(response, "client_id"),
                stringField(response, "tenant_id"),
                stringField(response, "identity_type"));
        if (isBlank(credential.certificate)
                || isBlank(credential.mtlsAuthenticationEndpoint)
                || isBlank(credential.clientId)
                || isBlank(credential.tenantId)
                || isBlank(credential.identityType)) {
            throw new MtlsMsiException(
                    "IMDS issuecredential returned an incomplete binding credential.");
        }
        return credential;
    }

    private static ManagedIdentityMtlsHttpResponse execute(
            ManagedIdentityMtlsRequest request,
            String method,
            String url,
            String body) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Metadata", "true");
        headers.put("x-ms-client-request-id", request.correlationId());
        if (body != null) {
            headers.put("Content-Type", "application/json");
        }
        ManagedIdentityMtlsHttpResponse response = request.httpClient().execute(
                new ManagedIdentityMtlsHttpRequest(
                        method, url, headers, body, false));
        if (response.statusCode() != 200) {
            throw new MtlsMsiException(
                    "IMDS " + method + " " + url + " failed with HTTP "
                            + response.statusCode() + ".");
        }
        return response;
    }

    private static void validateImdsOrigin(
            ManagedIdentityMtlsHttpResponse response) {
        for (Map.Entry<String, List<String>> header
                : response.headers().entrySet()) {
            if (header.getKey() == null
                    || !"server".equalsIgnoreCase(header.getKey())) {
                continue;
            }
            if (header.getValue() == null) {
                continue;
            }
            for (String value : header.getValue()) {
                String normalized = value == null
                        ? "" : value.trim().toUpperCase();
                if ("IMDS".equals(normalized)
                        || normalized.startsWith("IMDS/")) {
                    return;
                }
            }
        }
        throw new MtlsMsiException(
                "IMDS response did not contain the expected Server header.");
    }

    private static String buildUrl(String path, ManagedIdentityMtlsRequest request) {
        StringBuilder url = new StringBuilder(IMDS_BASE)
                .append(path)
                .append('?')
                .append(API_VERSION_QUERY);
        if (!isBlank(request.identityQueryParameter())) {
            url.append('&')
                    .append(encode(request.identityQueryParameter()))
                    .append('=')
                    .append(encode(request.identityQueryValue()));
        }
        return url.toString();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            throw new MtlsMsiException("Unable to encode IMDS query parameter.", e);
        }
    }

    private static Map<String, Object> parseResponse(String response) {
        try (JsonReader reader = JsonProviders.createReader(response)) {
            Map<String, Object> fields = reader.readMap(JsonReader::readUntyped);
            if (fields == null || reader.nextToken() != JsonToken.END_DOCUMENT) {
                throw new IOException("Expected one IMDS JSON object.");
            }
            return fields;
        } catch (IOException | IllegalStateException e) {
            throw new MtlsMsiException("Unable to parse the IMDS JSON response.", e);
        }
    }

    private static String stringField(Map<?, ?> fields, String name) {
        Object value = fields == null ? null : fields.get(name);
        return value instanceof String ? (String) value : null;
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    static final class PlatformMetadata {
        final String clientId;
        final String tenantId;
        final String vmId;
        final String vmssId;
        final String attestationEndpoint;

        PlatformMetadata(
                String clientId,
                String tenantId,
                String vmId,
                String vmssId,
                String attestationEndpoint) {
            this.clientId = clientId;
            this.tenantId = tenantId;
            this.vmId = vmId;
            this.vmssId = vmssId;
            this.attestationEndpoint = attestationEndpoint;
        }

        String cuId() {
            return isBlank(vmId) ? vmssId : vmId;
        }
    }

    static final class CredentialResponse {
        final String certificate;
        final String mtlsAuthenticationEndpoint;
        final String clientId;
        final String tenantId;
        final String identityType;

        CredentialResponse(
                String certificate,
                String mtlsAuthenticationEndpoint,
                String clientId,
                String tenantId,
                String identityType) {
            this.certificate = certificate;
            this.mtlsAuthenticationEndpoint = mtlsAuthenticationEndpoint;
            this.clientId = clientId;
            this.tenantId = tenantId;
            this.identityType = identityType;
        }
    }
}
