package com.microsoft.aad.msal4j.labapi2;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

public class LabUser implements JsonSerializable<LabUser> {
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
    private String tenantId;
    private String password;
    private String federationProvider;

    static LabUser fromJson(JsonReader jsonReader) throws IOException {
        LabUser user = new LabUser();

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
                    case "tenantId":
                        user.tenantId = reader.getString();
                        break;
                    case "federationProvider":
                        user.federationProvider = reader.getString();
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
        jsonWriter.writeStringField("tenantId", tenantId);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    public String getAppId() {
        return this.appId;
    }

    public String getUpn() {
        return this.upn;
    }

    public String getLabName() {
        return this.labName;
    }

    public String getB2cProvider() {
        return this.b2cProvider;
    }

    public String getUserType() {
        return this.userType;
    }

    public String getHomeDomain() {
        return this.homeDomain;
    }

    public String getHomeUPN() {
        return this.homeUPN;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    /**
     * Get the user's password, fetching from MSID Key Vault if necessary.
     *
     * @return The user's password
     */
    public String getPassword() {
        if (password == null || password.isEmpty()) {
            // Fetch from MSID Lab Key Vault
            password = LabUserHelper.fetchUserPassword(labName);
        }
        return password;
    }

    /**
     * Get the federation provider for this user.
     *
     * @return The federation provider string (e.g., "adfsv2019", "adfsv4", "none")
     */
    public String getFederationProvider() {
        return federationProvider != null ? federationProvider : "none";
    }

    /**
     * Set the federation provider for this user.
     *
     * @param federationProvider The federation provider string
     */
    public void setFederationProvider(String federationProvider) {
        this.federationProvider = federationProvider;
    }
}
