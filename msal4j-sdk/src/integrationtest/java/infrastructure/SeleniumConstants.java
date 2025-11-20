//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package infrastructure;

public class SeleniumConstants {

    static final String WEB_UPN_INPUT_ID = "i0116";
    static final String WEB_PASSWORD_ID = "i0118";
    static final String WEB_SUBMIT_ID = "idSIButton9";

    //ADFS
    static final String ADFS_UPN_INPUT_ID = "userNameInput";
    static final String ADFS_PASSWORD_ID = "passwordInput";
    static final String ADFS_SUBMIT_ID = "submitButton";

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
