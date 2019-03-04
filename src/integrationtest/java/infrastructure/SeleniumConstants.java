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

package infrastructure;

public class SeleniumConstants {

    public final static String WEB_UPN_INPUT_ID = "i0116";
    public final static String ADFSV4_WEB_PASSWORD_ID = "passwordInput";
    public final static String ADFSV4_WEB_SUBMIT_ID = "submitButton";
    public final static String WEB_PASSWORD_ID = "i0118";
    public final static String WEB_SUBMIT_ID = "idSIButton9";

    // ADFSv2 fields
    public final static String ADFSV2_WEB_USERNAME_INPUT_ID = "ContentPlaceHolder1_UsernameTextBox";
    public final static String ADFSV2_WEB_PASSWORD_INPUT_ID = "ContentPlaceHolder1_PasswordTextBox";
    public final static String ADFSV2_WEB_SUBMIT_BUTTON_ID = "ContentPlaceHolder1_SubmitButton";

    //MSAL B2C
    public final static String AUTHORITY_PICKER_ID = "b2cAuthorityPicker";
    public final static String WEB_UPN_B2C_LOCAL_INPUT_ID = "logonIdentifier";
    public final static String B2C_WEB_SUBMIT_ID = "next";
    public final static String B2C_WEB_PASSWORD_ID = "password";
    public final static String B2C_LOGIN_AUTHORITY = "b2clogin.com";
    public final static String MICROSOFT_ONLINE_AUTHORITY = "login.microsoftonline.com";
    public final static String NON_B2C_AUTHORITY = "non-b2c authority";
    public final static String B2C_EDIT_PROFILE_AUTHORITY = "Edit profile policy authority";
    public final static String FACEBOOK_ACCOUNT_ID = "FacebookExchange";
    public final static String WEB_UPN_B2C_FACEBOOK_INPUT_ID = "m_login_email";
    public final static String B2C_WEB_PASSWORD_FACEBOOK_ID = "m_login_password";
    public final static String B2C_FACEBOOK_SUBMIT_ID = "u_0_5";
    public final static String GOOGLE_ACCOUNT_ID = "GoogleExchange";
    public final static String WEB_UPN_B2C_GOOGLE_INPUT_ID = "Email";
    public final static String B2C_WEB_PASSWORD_GOOGLE_ID = "Passwd";
    public final static String B2C_GOOGLE_NEXT_ID = "next";
    public final static String B2C_GOOGLE_SIGNIN_ID = "signIn";
    public final static String B2C_EDIT_PROFILE_CONTINUE_ID = "continue";
}
