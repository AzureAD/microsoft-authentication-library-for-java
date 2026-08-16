// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.microsoft.aad.msal4j.ManagedIdentityMtlsHttpRequest;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsHttpResponse;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsRequest;

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
        String response = httpResponse.body();
        PlatformMetadata metadata = new PlatformMetadata(
                extractString(response, "clientId"),
                extractString(response, "tenantId"),
                extractNestedString(response, "cuId", "vmId"),
                extractNestedString(response, "cuId", "vmssId"),
                extractString(response, "attestationEndpoint"));
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
        String response = execute(
                request,
                "POST",
                buildUrl(ISSUE_CREDENTIAL_PATH, request),
                body).body();
        CredentialResponse credential = new CredentialResponse(
                extractString(response, "certificate"),
                extractString(response, "mtls_authentication_endpoint"),
                extractString(response, "client_id"),
                extractString(response, "tenant_id"),
                extractString(response, "identity_type"));
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
                new ManagedIdentityMtlsHttpRequest(method, url, headers, body));
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
                "IMDS getplatformmetadata response did not contain the expected Server header.");
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

    static String extractString(String json, String key) {
        if (json == null) {
            return null;
        }
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIndex + marker.length());
        if (colon < 0) {
            return null;
        }
        int quote = skipWhitespace(json, colon + 1);
        if (quote >= json.length() || json.charAt(quote) != '"') {
            return null;
        }
        StringBuilder value = new StringBuilder();
        for (int i = quote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') {
                return value.toString();
            }
            if (c == '\\' && i + 1 < json.length()) {
                char escaped = json.charAt(++i);
                value.append(escaped == 'n' ? '\n'
                        : escaped == 'r' ? '\r'
                        : escaped == 't' ? '\t' : escaped);
            } else {
                value.append(c);
            }
        }
        return null;
    }

    private static String extractNestedString(String json, String objectKey, String key) {
        String marker = "\"" + objectKey + "\"";
        int objectIndex = json == null ? -1 : json.indexOf(marker);
        if (objectIndex < 0) {
            return null;
        }
        int start = json.indexOf('{', objectIndex + marker.length());
        if (start < 0) {
            return null;
        }
        int end = json.indexOf('}', start + 1);
        return end < 0 ? null : extractString(json.substring(start, end + 1), key);
    }

    private static int skipWhitespace(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
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
