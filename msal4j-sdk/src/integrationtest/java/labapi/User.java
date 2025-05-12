// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

public class User implements JsonSerializable<User> {
    private String appId;
    private String objectId;
    private String userType;
    private String displayName;
    private String licenses;
    private String upn;
    private String mfa;
    private String protectionPolicy;
    private String homeDomain;
    private String homeUPN;
    private String b2cProvider;
    private String labName;
    private String lastUpdatedBy;
    private String lastUpdatedDate;
    private String tenantID;
    private String password;
    private String federationProvider;

    static User fromJson(JsonReader jsonReader) throws IOException {
        User user = new User();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "appId":
                        user.appId = reader.getString();
                        break;
                    case "objectId":
                        user.objectId = reader.getString();
                        break;
                    case "userType":
                        user.userType = reader.getString();
                        break;
                    case "displayName":
                        user.displayName = reader.getString();
                        break;
                    case "licenses":
                        user.licenses = reader.getString();
                        break;
                    case "upn":
                        user.upn = reader.getString();
                        break;
                    case "mfa":
                        user.mfa = reader.getString();
                        break;
                    case "protectionPolicy":
                        user.protectionPolicy = reader.getString();
                        break;
                    case "homeDomain":
                        user.homeDomain = reader.getString();
                        break;
                    case "homeUPN":
                        user.homeUPN = reader.getString();
                        break;
                    case "b2cProvider":
                        user.b2cProvider = reader.getString();
                        break;
                    case "labName":
                        user.labName = reader.getString();
                        break;
                    case "lastUpdatedBy":
                        user.lastUpdatedBy = reader.getString();
                        break;
                    case "lastUpdatedDate":
                        user.lastUpdatedDate = reader.getString();
                        break;
                    case "tenantID":
                        user.tenantID = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return user;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        jsonWriter.writeStringField("appId", appId);
        jsonWriter.writeStringField("objectId", objectId);
        jsonWriter.writeStringField("userType", userType);
        jsonWriter.writeStringField("displayName", displayName);
        jsonWriter.writeStringField("licenses", licenses);
        jsonWriter.writeStringField("upn", upn);
        jsonWriter.writeStringField("mfa", mfa);
        jsonWriter.writeStringField("protectionPolicy", protectionPolicy);
        jsonWriter.writeStringField("homeDomain", homeDomain);
        jsonWriter.writeStringField("homeUPN", homeUPN);
        jsonWriter.writeStringField("b2cProvider", b2cProvider);
        jsonWriter.writeStringField("labName", labName);
        jsonWriter.writeStringField("lastUpdatedBy", lastUpdatedBy);
        jsonWriter.writeStringField("lastUpdatedDate", lastUpdatedDate);
        jsonWriter.writeStringField("tenantID", tenantID);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    public String getAppId() {
        return this.appId;
    }

    public String getUserType() {
        return this.userType;
    }

    public String getUpn() {
        return this.upn;
    }

    public String getHomeDomain() {
        return this.homeDomain;
    }

    public String getHomeUPN() {
        return this.homeUPN;
    }

    public String getB2cProvider() {
        return this.b2cProvider;
    }

    public String getLabName() {
        return this.labName;
    }

    public String getTenantID() {
        return this.tenantID;
    }

    public String getPassword() {
        return this.password;
    }

    public String getFederationProvider() {
        return this.federationProvider;
    }

    public void setUpn(String upn) {
        this.upn = upn;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFederationProvider(String federationProvider) {
        this.federationProvider = federationProvider;
    }
}