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
        return SeleniumConstants.WEBSUBMITID;
    }

    String getAadUserNameInputId() {
        return SeleniumConstants.WEBUPNINPUTID;
    }

    private void determineFieldIds(){
        if(labUser.isFederated()){
            if(labUser.getFederationProvider() == FederationProvider.ADFSV2){
                passwordInputId = SeleniumConstants.ADFSV2WEBPASSWORDINPUTID;
                passwordSigInButtonId = SeleniumConstants.ADFSV2WEBSUBMITBUTTONID;
                return;
            }
            passwordInputId = SeleniumConstants.ADFSV4WEBPASSWORDID;
            passwordSigInButtonId = SeleniumConstants.ADFSV4WEBSUBMITID;
            return;
        }
        if(labUser.getUserType() == UserType.B2C){
            determineFieldIds();
            return;
        }
        passwordInputId = SeleniumConstants.WEBPASSWORDID;
        passwordSigInButtonId = SeleniumConstants.WEBSUBMITID;
    }


    private void determinB2cFieldIds(){
        switch(labUser.getB2CIdentityProvider()){
            case LOCAL:
                passwordSigInButtonId = SeleniumConstants.B2CWEBPASSWORDID;
                passwordInputId = SeleniumConstants.B2CWEBSUBMITID;
                break;
            case FACEBOOK:
                passwordSigInButtonId = SeleniumConstants.B2CWEBPASSWORDFACEBOOKID;
                passwordInputId = SeleniumConstants.B2CFACEBOOKSUBMITID;
                break;
            case GOOGLE:
                passwordSigInButtonId = SeleniumConstants.B2CWEBPASSWORDGOOGLEID;
                passwordInputId = SeleniumConstants.B2CGOOGLESIGNINID;
                break;
        }
    }
}
