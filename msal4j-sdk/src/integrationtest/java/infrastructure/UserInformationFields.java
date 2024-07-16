// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import labapi.FederationProvider;
import labapi.LabConstants;
import labapi.User;

class UserInformationFields {
    private final User user;
    private String passwordInputId;
    private String passwordSigInButtonId;

    UserInformationFields(User labUser) {
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
        switch (user.getFederationProvider()) {
            case FederationProvider.ADFS_2019:
                passwordInputId = SeleniumConstants.ADFS2019_PASSWORD_ID;
                passwordSigInButtonId = SeleniumConstants.ADFS2019_SUBMIT_ID;
                break;
            case FederationProvider.ADFS_4:
                passwordInputId = SeleniumConstants.ADFSV4_WEB_PASSWORD_ID;
                passwordSigInButtonId = SeleniumConstants.ADFSV4_WEB_SUBMIT_ID;
                break;
            default:
                passwordInputId = SeleniumConstants.WEB_PASSWORD_ID;
                passwordSigInButtonId = SeleniumConstants.WEB_SUBMIT_ID;
        }
    }
}
