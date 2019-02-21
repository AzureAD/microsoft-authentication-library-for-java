//----------------------------------------------------------------------
//
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
//
//------------------------------------------------------------------------------

package Infrastructure;

public class SeleniumConstants {
    // Resources
    public final static String MSGRAPH = "https://graph.microsoft.com";
    public final static String EXCHANGE = "https://outlook.office365.com/";

    //MSAL test app
    public final static String ACQUIRETOKENBUTTONID = "acquireToken_button";
    public final static String ACQUIRETOKENWITHPROMPTBEHAVIORALWAYSID = "acquireTokenPromptBehaviorAlways";
    public final static String ACQUIRETOKENSILENTBUTTONID = "acquireTokenSilent_button";
    public final static String CLIENTIDENTRYID = "clientIdEntry";
    public final static String RESOURCEENTRYID = "resourceEntry";
    public final static String SECONDPAGEID = "secondPage";
    public final static String CLEARCACHEID = "clearCache";
    public final static String SAVEID = "saveButton";
    public final static String WEBUPNINPUTID = "i0116";
    public final static String ADFSV4WEBPASSWORDID = "passwordInput";
    public final static String ADFSV4WEBSUBMITID = "submitButton";
    public final static String WEBPASSWORDID = "i0118";
    public final static String WEBSUBMITID = "idSIButton9";
    public final static String TESTRESULTID = "testResult";
    public final static String TESTRESULTSUCCESSFULMESSAGE = "Result: Success";
    public final static String TESTRESULTFAILUREMESSAGE = "Result: Failure";
    public final static String CLEARALLCACHEID = "ClearAllCache";

    public final static String DEFAULTSCOPE = "User.Read";
    public final static String ACQUIREPAGEID = "AcquirePage";
    public final static String CACHEPAGEID = "CachePage";
    public final static String SETTINGSPAGEID = "SettingsPage";
    public final static String LOGPAGEID = "LogPage";
    public final static String ACQUIREPAGEANDROIDID = "Acquire";
    public final static String CACHEPAGEANDROIDID = "Cache";
    public final static String SETTINGSPAGEANDROIDID = "Settings";
    public final static String LOGPAGEANDROIDID = "Log";
    public final static String SCOPESENTRYID = "scopesList";
    public final static String UIBEHAVIORPICKERID = "uiBehavior";
    public final static String SELECTUSER = "userList";
    public final static String USERNOTSELECTED = "not selected";
    public final static String USERMISSINGFROMRESPONSE = "Missing from the token response";
    public final static String REDIRECTURIONANDROID = "urn:ietf:wg:oauth:2.0:oob";
    public final static String REDIRECTURIENTRYID = "redirectUriEntry";

    // ADFSv2 fields
    public final static String ADFSV2WEBUSERNAMEINPUTID = "ContentPlaceHolder1_UsernameTextBox";
    public final static String ADFSV2WEBPASSWORDINPUTID = "ContentPlaceHolder1_PasswordTextBox";
    public final static String ADFSV2WEBSUBMITBUTTONID = "ContentPlaceHolder1_SubmitButton";

    //MSAL B2C
    public final static String AUTHORITYPICKERID = "b2cAuthorityPicker";
    public final static String WEBUPNB2CLOCALINPUTID = "logonIdentifier";
    public final static String B2CWEBSUBMITID = "next";
    public final static String B2CWEBPASSWORDID = "password";
    public final static String B2CLOGINAUTHORITY = "b2clogin.com";
    public final static String MICROSOFTONLINEAUTHORITY = "login.microsoftonline.com";
    public final static String NONB2CAUTHORITY = "non-b2c authority";
    public final static String B2CEDITPROFILEAUTHORITY = "Edit profile policy authority";
    public final static String FACEBOOKACCOUNTID = "FacebookExchange";
    public final static String WEBUPNB2CFACEBOOKINPUTID = "m_login_email";
    public final static String B2CWEBPASSWORDFACEBOOKID = "m_login_password";
    public final static String B2CFACEBOOKSUBMITID = "u_0_5";
    public final static String GOOGLEACCOUNTID = "GoogleExchange";
    public final static String WEBUPNB2CGOOGLEINPUTID = "Email";
    public final static String B2CWEBPASSWORDGOOGLEID = "Passwd";
    public final static String B2CGOOGLENEXTID = "next";
    public final static String B2CGOOGLESIGNINID = "signIn";
    public final static String B2CEDITPROFILECONTINUEID = "continue";

    // these should match the product enum values
    public final static String UIBEHAVIORCONSENT = "consent";
    public final static String UIBEHAVIORSELECTACCOUNT = "select_account";
    public final static String UIBEHAVIORLOGIN = "login";
    public final static String UIBEHAVIORNOPROMPT = "no_prompt";

    // Test Constants
    public final static int RESULTCHECKPOLLIINTERVAL = 1000;
    public final static int MAXIMUMRESULTCHECKRETRYATTEMPTS = 20;

}
