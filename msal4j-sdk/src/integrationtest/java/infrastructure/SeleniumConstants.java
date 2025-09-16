//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package infrastructure;

public class SeleniumConstants {
    //ADFS v4
    static final String ADFSV4_WEB_PASSWORD_ID = "passwordInput";
    static final String ADFSV4_WEB_SUBMIT_ID = "submitButton";

    static final String WEB_UPN_INPUT_ID = "i0116";
    static final String WEB_PASSWORD_ID = "i0118";
    static final String WEB_SUBMIT_ID = "idSIButton9";

    //ADFS2019
    static final String ADFS2019_UPN_INPUT_ID = "userNameInput";
    static final String ADFS2019_PASSWORD_ID = "passwordInput";
    static final String ADFS2019_SUBMIT_ID = "submitButton";

    //B2C Facebook
    static final String FACEBOOK_ACCOUNT_ID = "FacebookExchange";
    static final String FACEBOOK_USERNAME_ID = "email";
    static final String FACEBOOK_PASSWORD_ID = "pass";
    static final String FACEBOOK_LOGIN_BUTTON_ID = "loginbutton";

    //B2C Google
    static final String GOOGLE_ACCOUNT_ID = "GoogleExchange";
    static final String GOOGLE_USERNAME_ID = "identifierId";
    static final String GOOGLE_NEXT_AFTER_USERNAME_BUTTON = "identifierNext";
    static final String GOOGLE_PASSWORD_ID = "password";
    static final String GOOGLE_NEXT_BUTTON_ID = "passwordNext";

    // B2C Local
    static final String B2C_LOCAL_ACCOUNT_ID = "SignInWithLogonNameExchange";
    static final String B2C_LOCAL_USERNAME_ID = "cred_userid_inputtext";
    static final String B2C_LOCAL_PASSWORD_ID = "cred_password_inputtext";
    static final String B2C_LOCAL_SIGN_IN_BUTTON_ID = "cred_sign_in_button";

    // Stay signed in?
    static final String STAY_SIGN_IN_NO_BUTTON_ID = "idBtn_Back";

    // Are you trying to sign in to ...
    //Only continue if you downloaded the app from a store or website that you trust.
    static final String ARE_YOU_TRYING_TO_SIGN_IN_TO = "idSIButton9";
}
