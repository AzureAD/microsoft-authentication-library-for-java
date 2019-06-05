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

    final static String WEB_UPN_INPUT_ID = "i0116";
    final static String ADFSV4_WEB_PASSWORD_ID = "passwordInput";
    final static String ADFSV4_WEB_SUBMIT_ID = "submitButton";
    final static String WEB_PASSWORD_ID = "i0118";
    final static String WEB_SUBMIT_ID = "idSIButton9";

    // ADFSv2 fields
    final static String ADFSV2_WEB_USERNAME_INPUT_ID = "ContentPlaceHolder1_UsernameTextBox";
    final static String ADFSV2_WEB_PASSWORD_INPUT_ID = "ContentPlaceHolder1_PasswordTextBox";
    final static String ADFSV2_WEB_SUBMIT_BUTTON_ID = "ContentPlaceHolder1_SubmitButton";

    //B2C Facebook
    final static String FACEBOOK_ACCOUNT_ID = "FacebookExchange";
    final static String FACEBOOK_USERNAME_ID = "email";
    final static String FACEBOOK_PASSWORD_ID = "pass";
    final static String FACEBOOK_LOGIN_BUTTON_ID = "loginbutton";

    //B2C Google
    final static String GOOGLE_ACCOUNT_ID = "GoogleExchange";
    final static String GOOGLE_USERNAME_ID = "identifierId";
    final static String GOOGLE_NEXT_AFTER_USERNAME_BUTTON = "identifierNext";
    final static String GOOGLE_PASSWORD_ID = "password";
    final static String GOOGLE_NEXT_BUTTON_ID = "passwordNext";

    // B2C Local
    final static String B2C_LOCAL_ACCOUNT_ID = "SignInWithLogonNameExchange";
    final static String B2C_LOCAL_USERNAME_ID = "cred_userid_inputtext";
    final static String B2C_LOCAL_PASSWORD_ID = "cred_password_inputtext";
    final static String B2C_LOCAL_SIGN_IN_BUTTON_ID = "cred_sign_in_button";
}
