// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonWriter;
import com.azure.json.JsonSerializable;

import java.io.IOException;

/**
 * Request body for the IMDS {@code POST /metadata/identity/issuecredential} endpoint.
 * Used in Step 5 of the MSI v2 mTLS PoP flow to obtain a short-lived mTLS client certificate.
 */
class IssueCertificateRequest implements JsonSerializable<IssueCertificateRequest> {

    /** Base64-encoded PKCS#10 CSR signed with the KeyGuard RSA key. */
    String csr;

    /** JWT attestation token from the KeyGuard attestation service. */
    String attestationToken;

    IssueCertificateRequest(String csr, String attestationToken) {
        this.csr = csr;
        this.attestationToken = attestationToken;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeStringField("csr", csr);
        jsonWriter.writeStringField("attestation_token", attestationToken);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }
}
