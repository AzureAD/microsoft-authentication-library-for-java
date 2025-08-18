// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

/**
 * Response returned from the STS device code endpoint containing information necessary for
 * device code flow
 */
public final class DeviceCode implements JsonSerializable<DeviceCode> {

    private String userCode;
    private String deviceCode;
    private String verificationUri;
    private long expiresIn;
    private long interval;
    private String message;

    private String correlationId = null;
    private String clientId = null;
    private String scopes = null;

    public static DeviceCode fromJson(JsonReader jsonReader) throws IOException {
        DeviceCode deviceCode = new DeviceCode();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "user_code":
                        deviceCode.userCode = reader.getString();
                        break;
                    case "device_code":
                        deviceCode.deviceCode = reader.getString();
                        break;
                    case "verification_uri":
                        deviceCode.verificationUri = reader.getString();
                        break;
                    case "expires_in":
                        deviceCode.expiresIn = reader.getLong();
                        break;
                    case "interval":
                        deviceCode.interval = reader.getLong();
                        break;
                    case "message":
                        deviceCode.message = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return deviceCode;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        jsonWriter.writeStringField("user_code", userCode);
        jsonWriter.writeStringField("device_code", deviceCode);
        jsonWriter.writeStringField("verification_uri", verificationUri);
        jsonWriter.writeNumberField("expires_in", expiresIn);
        jsonWriter.writeNumberField("interval", interval);
        jsonWriter.writeStringField("message", message);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    /**
     * code which user needs to provide when authenticating at the verification URI
     */
    public String userCode() {
        return this.userCode;
    }

    /**
     * code which should be included in the request for the access token
     */
    public String deviceCode() {
        return this.deviceCode;
    }

    /**
     * URI where user can authenticate
     */
    public String verificationUri() {
        return this.verificationUri;
    }

    /**
     * expiration time of device code in seconds.
     */
    public long expiresIn() {
        return this.expiresIn;
    }

    /**
     * interval at which the STS should be polled at
     */
    public long interval() {
        return this.interval;
    }

    /**
     * message which should be displayed to the user.
     */
    public String message() {
        return this.message;
    }

    protected String correlationId() {
        return this.correlationId;
    }

    protected String clientId() {
        return this.clientId;
    }

    protected String scopes() {
        return this.scopes;
    }

    protected DeviceCode correlationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    protected DeviceCode clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    protected DeviceCode scopes(String scopes) {
        this.scopes = scopes;
        return this;
    }
}