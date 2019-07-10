// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import labapi.FederationProvider;
import labapi.LabUser;
import labapi.UserType;
import org.testng.util.Strings;

class UserInformationFields {
    private final LabUser labUser;
    private String passwordInputId;
    private String passwordSigInButtonId;

    UserInformationFields(LabUser labUser){
        this.labUser = labUser;
    }

    String getPasswordInputId() {
        if(Strings.isNullOrEmpty(passwordInputId)){
            determineFieldIds();
        }
        return passwordInputId;
    }

    String getPasswordSigInButtonId() {
        if(Strings.isNullOrEmpty(passwordSigInButtonId)){
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

    private void determineFieldIds(){
        if(labUser.isFederated()){
            if(labUser.getFederationProvider() == FederationProvider.ADFSV2){
                passwordInputId = SeleniumConstants.ADFSV2_WEB_PASSWORD_INPUT_ID;
                passwordSigInButtonId = SeleniumConstants.ADFSV2_WEB_SUBMIT_BUTTON_ID;
                return;
            }
            passwordInputId = SeleniumConstants.ADFSV4_WEB_PASSWORD_ID;
            passwordSigInButtonId = SeleniumConstants.ADFSV4_WEB_SUBMIT_ID;
            return;
        }
        passwordInputId = SeleniumConstants.WEB_PASSWORD_ID;
        passwordSigInButtonId = SeleniumConstants.WEB_SUBMIT_ID;
    }
}
