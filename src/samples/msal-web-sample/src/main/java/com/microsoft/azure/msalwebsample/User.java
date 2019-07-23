// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.msalwebsample;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

@Getter
@Setter
public class User {

    protected String displayName;
    protected String givenName;
    protected String id;
    protected String jobTitle;
    protected String mail;
    protected String mobilePhone;
    protected String officeLocation;
    protected String preferredLanguage;
    protected String surname;
    protected String userPrincipalName;

    @Override
    public String toString() {
        return new JSONObject(this).toString();
    }
}



