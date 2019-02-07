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

public class UserInformationFields {
    private final LabUser labUser;
    private String passwordInputId;
    private String passwordSigInButtonId;

    public UserInformationFields(LabUser labUser){
        this.labUser = labUser;
    }

    public String getPasswordInputId() {
        if(Strings.isNullOrEmpty(passwordInputId)){
            determineFieldIds();
        }
        return passwordInputId;
    }

    public String getPasswordSigInButtonId() {
        if(Strings.isNullOrEmpty(passwordSigInButtonId)){
            determineFieldIds();
        }
        return passwordSigInButtonId;
    }

    public String getAadSignInButtonId() {
        return UiTestConstants.WEBSUBMITID;
    }

    public String getAadUserNameInputId() {
        return UiTestConstants.WEBUPNINPUTID;
    }

    private void determineFieldIds(){
        if(labUser.isFederated()){
            if(labUser.getFederationProvider() == FederationProvider.ADFSV2){
                passwordInputId = UiTestConstants.ADFSV2WEBPASSWORDINPUTID;
                passwordSigInButtonId = UiTestConstants.ADFSV2WEBSUBMITBUTTONID;
                return;
            }
            passwordInputId = UiTestConstants.ADFSV4WEBPASSWORDID;
            passwordSigInButtonId = UiTestConstants.ADFSV4WEBSUBMITID;
        }
        if(labUser.getUserType() == UserType.B2C){
            determineFieldIds();
            return;
        }
        passwordInputId = UiTestConstants.WEBPASSWORDID;
        passwordSigInButtonId = UiTestConstants.WEBSUBMITID;
    }


    private void determinB2cFieldIds(){
        switch(labUser.getB2CIdentityProvider()){
            case LOCAL:
                passwordSigInButtonId = UiTestConstants.B2CWEBPASSWORDID;
                passwordInputId = UiTestConstants.B2CWEBSUBMITID;
                break;
            case FACEBOOK:
                passwordSigInButtonId = UiTestConstants.B2CWEBPASSWORDFACEBOOKID;
                passwordInputId = UiTestConstants.B2CFACEBOOKSUBMITID;
                break;
            case GOOGLE:
                passwordSigInButtonId = UiTestConstants.B2CWEBPASSWORDGOOGLEID;
                passwordInputId = UiTestConstants.B2CGOOGLESIGNINID;
                break;
        }
    }
}
