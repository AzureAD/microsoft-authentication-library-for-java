// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Getter
@Setter
public class User {
    @JsonProperty("appId")
    private String appId;

    @JsonProperty("objectId")
    private String objectId;

    @JsonProperty("userType")
    private String userType;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("licenses")
    private String licenses;

    @JsonProperty("upn")
    @Setter
    private String upn;

    @JsonProperty("mfa")
    private String mfa;

    @JsonProperty("protectionPolicy")
    private String protectionPolicy;

    @JsonProperty("homeDomain")
    private String homeDomain;

    @JsonProperty("homeUPN")
    private String homeUPN;

    @JsonProperty("b2cProvider")
    private String b2cProvider;

    @JsonProperty("labName")
    private String labName;

    @JsonProperty("lastUpdatedBy")
    private String lastUpdatedBy;

    @JsonProperty("lastUpdatedDate")
    private String lastUpdatedDate;

    @Setter
    private String password;

    @Setter
    private String federationProvider;

    public static User convertJsonToObject(String json) throws IOException {
        if (json != null) {
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jsonParser = jsonFactory.createParser(json);

            User user = new User();

            if (jsonParser.nextToken().equals(JsonToken.START_ARRAY)) {
                jsonParser.nextToken();
            }

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jsonParser.getCurrentName();
                if ("appId".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setAppId(jsonParser.getText());
                }

                else if ("objectId".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setObjectId(jsonParser.getText());
                }

                else if ("userType".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setUserType(jsonParser.getText());
                }

                else if ("displayName".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setDisplayName(jsonParser.getText());
                }

                else if ("licenses".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setLicenses(jsonParser.getText());
                }

                else if ("upn".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setUpn(jsonParser.getText());
                }

                else if ("mfa".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setMfa(jsonParser.getText());
                }

                else if ("protectionPolicy".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setProtectionPolicy(jsonParser.getText());
                }

                else if ("homeDomain".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setHomeDomain(jsonParser.getText());
                }

                else if ("homeUPN".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setHomeUPN(jsonParser.getText());
                }

                else if ("b2cProvider".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setB2cProvider(jsonParser.getText());
                }

                else if ("labName".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setLabName(jsonParser.getText());
                }

                else if ("lastUpdatedBy".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setLastUpdatedBy(jsonParser.getText());
                }

                else if ("lastUpdatedDate".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setLastUpdatedDate(jsonParser.getText());
                }

                else if ("password".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setPassword(jsonParser.getText());
                }

                else if ("federationProvider".equals(fieldname)) {
                    jsonParser.nextToken();
                    user.setFederationProvider(jsonParser.getText());
                }

            }

            return user;
        }

        return new User();
    }
}
