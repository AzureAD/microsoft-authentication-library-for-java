// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.msalwebsample;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("b2c")
class BasicConfiguration {
    String clientId;
    String secret;
    String redirectUri;

    String api;
    String apiScope;

    String signUpSignInAuthority;
    String editProfileAuthority;
    String resetPasswordAuthority;
}