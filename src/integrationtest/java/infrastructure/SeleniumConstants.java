//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package infrastructure;

public class SeleniumConstants {
    //ADFS v4
    final static String ADFSV4_WEB_PASSWORD_ID = "passwordInput";
    final static String ADFSV4_WEB_SUBMIT_ID = "submitButton";

    final static String WEB_UPN_INPUT_ID = "i0116";
    final static String WEB_PASSWORD_ID = "i0118";
    final static String WEB_SUBMIT_ID = "idSIButton9";

    //ADFS2019
    final static String ADFS2019_UPN_INPUT_ID = "userNameInput";
    final static String ADFS2019_PASSWORD_ID = "passwordInput";
    final static String ADFS2019_SUBMIT_ID = "submitButton";

    // ADFSv2 fields
    final static String ADFSV2_WEB_USERNAME_INPUT_ID = "ContentPlaceHolder1_UsernameTextBox";
    final static String ADFSV2_WEB_PASSWORD_INPUT_ID = "ContentPlaceHolder1_PasswordTextBox";
    final static String ADFSV2_ARLINGTON_WEB_PASSWORD_INPUT_ID = "passwordInput";
    final static String ADFSV2_WEB_SUBMIT_BUTTON_ID = "ContentPlaceHolder1_SubmitButton";
    final static String ADFSV2_ARLINGTON_WEB_SUBMIT_BUTTON_ID = "submitButton";


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

    // Stay signed in?
    final static String STAY_SIGN_IN_NO_BUTTON_ID = "idBtn_Back";
}
