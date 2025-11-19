// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import com.microsoft.aad.msal4j.labapi2.LabUser;

class UserInformationFields {
    private final LabUser user;
    private String passwordInputId;
    private String passwordSigInButtonId;

    UserInformationFields(LabUser labUser) {
        this.user = labUser;
    }

    String getPasswordInputId() {
        if (passwordInputId == null || passwordInputId.equals("")) {
            determineFieldIds();
        }
        return passwordInputId;
    }

    String getPasswordSigInButtonId() {
        if (passwordSigInButtonId == null || passwordSigInButtonId.equals("")) {
            determineFieldIds();
        }
        return passwordSigInButtonId;
    }

    String getAadSignInButtonId() {
        return SeleniumConstants.WEB_SUBMIT_ID;
    }

    String getAadUserNameInputId() {
        return SeleniumConstants.WEB_UPN_INPUT_ID;
    }

    String getADFS2019UserNameInputId() {
        return SeleniumConstants.ADFS2019_UPN_INPUT_ID;
    }

    private void determineFieldIds() {
        String federationProvider = user.getFederationProvider();

        if ("adfsv2019".equalsIgnoreCase(federationProvider)) {
            passwordInputId = SeleniumConstants.ADFS2019_PASSWORD_ID;
            passwordSigInButtonId = SeleniumConstants.ADFS2019_SUBMIT_ID;
        } else if ("adfsv4".equalsIgnoreCase(federationProvider)) {
            passwordInputId = SeleniumConstants.ADFSV4_WEB_PASSWORD_ID;
            passwordSigInButtonId = SeleniumConstants.ADFSV4_WEB_SUBMIT_ID;
        } else {
            passwordInputId = SeleniumConstants.WEB_PASSWORD_ID;
            passwordSigInButtonId = SeleniumConstants.WEB_SUBMIT_ID;
        }
    }
}
