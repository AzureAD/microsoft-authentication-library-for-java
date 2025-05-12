// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.io.Serializable;

class IdToken implements Serializable, JsonSerializable<IdToken> {

    protected String issuer;
    protected String subject;
    protected String audience;
    protected Long expirationTime;
    protected Long issuedAt;
    protected Long notBefore;
    protected String name;
    protected String preferredUsername;
    protected String objectIdentifier;
    protected String tenantIdentifier;
    protected String upn;
    protected String uniqueName;

    static IdToken fromJson(JsonReader jsonReader) throws IOException {
        IdToken idToken = new IdToken();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "iss":
                        idToken.issuer = reader.getString();
                        break;
                    case "sub":
                        idToken.subject = reader.getString();
                        break;
                    case "aud":
                        idToken.audience = reader.getString();
                        break;
                    case "exp":
                        idToken.expirationTime = reader.getLong();
                        break;
                    case "iat":
                        idToken.issuedAt = reader.getLong();
                        break;
                    case "nbf":
                        idToken.notBefore = reader.getLong();
                        break;
                    case "name":
                        idToken.name = reader.getString();
                        break;
                    case "preferred_username":
                        idToken.preferredUsername = reader.getString();
                        break;
                    case "oid":
                        idToken.objectIdentifier = reader.getString();
                        break;
                    case "tid":
                        idToken.tenantIdentifier = reader.getString();
                        break;
                    case "upn":
                        idToken.upn = reader.getString();
                        break;
                    case "unique_name":
                        idToken.uniqueName = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return idToken;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        jsonWriter.writeStringField("iss", issuer);
        jsonWriter.writeStringField("sub", subject);
        jsonWriter.writeStringField("aud", audience);
        jsonWriter.writeNumberField("exp", expirationTime);
        jsonWriter.writeNumberField("iat", issuedAt);
        jsonWriter.writeNumberField("nbf", notBefore);
        jsonWriter.writeStringField("name", name);
        jsonWriter.writeStringField("preferred_username", preferredUsername);
        jsonWriter.writeStringField("oid", objectIdentifier);
        jsonWriter.writeStringField("tid", tenantIdentifier);
        jsonWriter.writeStringField("upn", upn);
        jsonWriter.writeStringField("unique_name", uniqueName);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }
}