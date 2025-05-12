// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Represents an individual requested claims that's part of a complete claims request parameter
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
public class RequestedClaim implements JsonSerializable<RequestedClaim> {

    public String name;
    private RequestedClaimAdditionalInfo requestedClaimAdditionalInfo;

    RequestedClaim() {}

    public RequestedClaim(String name, RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
        this.name = name;
        this.requestedClaimAdditionalInfo = requestedClaimAdditionalInfo;
    }

    static RequestedClaim fromJson(JsonReader jsonReader) throws IOException {
        RequestedClaim claim = new RequestedClaim();
        return jsonReader.readObject(reader -> {
            if (reader.currentToken() != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected start of object but was " + reader.currentToken());
            }

            claim.name = reader.getFieldName();

            RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(false, null, null);
            claim.requestedClaimAdditionalInfo = info.fromJson(reader);

            return claim;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        if (name != null && requestedClaimAdditionalInfo != null) {
            jsonWriter.writeString(name);
            requestedClaimAdditionalInfo.toJson(jsonWriter);
        }

        jsonWriter.writeEndObject();
        return jsonWriter;
    }

    RequestedClaimAdditionalInfo getRequestedClaimAdditionalInfo() {
        return requestedClaimAdditionalInfo;
    }

    void setRequestedClaimAdditionalInfo(RequestedClaimAdditionalInfo requestedClaimAdditionalInfo) {
        this.requestedClaimAdditionalInfo = requestedClaimAdditionalInfo;
    }

    protected Map<String, Object> any() {
        return Collections.singletonMap(name, requestedClaimAdditionalInfo);
    }
}