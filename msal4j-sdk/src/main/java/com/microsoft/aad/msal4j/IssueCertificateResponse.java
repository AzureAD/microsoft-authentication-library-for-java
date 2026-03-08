// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

/**
 * Response DTO from the IMDS {@code POST /metadata/identity/issuecredential} endpoint.
 * Contains the short-lived X.509 certificate to use for mTLS token acquisition.
 * Used in Step 6 of the MSI v2 mTLS PoP flow.
 */
class IssueCertificateResponse implements JsonSerializable<IssueCertificateResponse> {

    /** Base64-encoded DER X.509 certificate issued by IMDS for mTLS. */
    String certificate;

    /** Regional ESTS mTLS endpoint URL to acquire the final PoP token from. */
    String mtlsAuthenticationEndpoint;

    /** Tenant ID associated with the managed identity. */
    String tenantId;

    /** Client ID of the managed identity. */
    String clientId;

    public static IssueCertificateResponse fromJson(JsonReader jsonReader) throws IOException {
        IssueCertificateResponse response = new IssueCertificateResponse();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                switch (fieldName) {
                    case "certificate":
                        response.certificate = reader.getString();
                        break;
                    case "mtls_authentication_endpoint":
                        response.mtlsAuthenticationEndpoint = reader.getString();
                        break;
                    case "tenant_id":
                        response.tenantId = reader.getString();
                        break;
                    case "client_id":
                        response.clientId = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return response;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeStringField("certificate", certificate);
        jsonWriter.writeStringField("mtls_authentication_endpoint", mtlsAuthenticationEndpoint);
        jsonWriter.writeStringField("tenant_id", tenantId);
        jsonWriter.writeStringField("client_id", clientId);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }
}
