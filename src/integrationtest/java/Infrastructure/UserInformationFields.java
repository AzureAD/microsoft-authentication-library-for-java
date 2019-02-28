// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package Infrastructure;

import lapapi.FederationProvider;
import lapapi.LabUser;
import lapapi.UserType;
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
        if(labUser.getUserType() == UserType.B2C){
            determineB2cFieldIds();
            return;
        }
        passwordInputId = SeleniumConstants.WEB_PASSWORD_ID;
        passwordSigInButtonId = SeleniumConstants.WEB_SUBMIT_ID;
    }


    private void determineB2cFieldIds(){
        switch(labUser.getB2CIdentityProvider()){
            case LOCAL:
                passwordSigInButtonId = SeleniumConstants.B2C_WEB_PASSWORD_ID;
                passwordInputId = SeleniumConstants.B2C_WEB_SUBMIT_ID;
                break;
            case FACEBOOK:
                passwordSigInButtonId = SeleniumConstants.B2C_WEB_PASSWORD_FACEBOOK_ID;
                passwordInputId = SeleniumConstants.B2C_FACEBOOK_SUBMIT_ID;
                break;
            case GOOGLE:
                passwordSigInButtonId = SeleniumConstants.B2C_WEB_PASSWORD_GOOGLE_ID;
                passwordInputId = SeleniumConstants.B2C_GOOGLE_SIGNIN_ID;
                break;
        }
    }
}
