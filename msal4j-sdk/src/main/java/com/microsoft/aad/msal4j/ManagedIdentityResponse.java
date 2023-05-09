// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Getter
public class ManagedIdentityResponse {

    private final static Logger LOG = LoggerFactory.getLogger(ManagedIdentityResponse.class);
    @JsonProperty(value = "token_type")
    String tokenType;

    @JsonProperty(value = "access_token")
    String accessToken;

    @JsonProperty(value = "expires_on")
    String expiresOn;

    String resource;

    @JsonProperty(value = "client_id")
    String clientId;

    /**
     * Creates an access token instance.
     *
     * @param token the token string.
     * @param expiresOn the expiration time.
     */
    @JsonCreator
    private ManagedIdentityResponse(
            @JsonProperty(value = "access_token") String token,
            @JsonProperty(value = "expires_on") String expiresOn,
            @JsonProperty(value = "expires_in") String expiresIn) {
//        super(token, Instant.now().EPOCH.plusSeconds(parseDateToEpochSeconds((StringHelper.isNullOrBlank(expiresOn) ? expiresIn
//                : expiresOn))));
        this.accessToken = token;
        this.expiresOn =  expiresOn;
        this.expiresOn = expiresIn;
    }

    private static Long parseDateToEpochSeconds(String dateTime) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("M/d/yyyy H:mm:ss XXX").withLocale(Locale.US);
        // This is the format for app service on Windows as of API version 2017-09-01.
        // The format is changed to Unix timestamp in 2019-08-01 but this API version
        // has not been deployed to Linux app services.
        DateTimeFormatter dtfWindows = DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a XXX").withLocale(Locale.US);
        try {
            return Long.parseLong(dateTime);
        } catch (NumberFormatException e) {
            LOG.warn(e.getMessage());
        }

        try {
            return Instant.from(dtf.parse(dateTime)).getEpochSecond();
        } catch (DateTimeParseException e) {
            LOG.warn(e.getMessage());
        }

        try {
            return Instant.from(dtfWindows.parse(dateTime)).getEpochSecond();
        } catch (DateTimeParseException e) {
            LOG.warn(e.getMessage());
        }

        throw new IllegalArgumentException("Unable to parse date time " + dateTime);
    }

}
