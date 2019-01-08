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

package com.microsoft.aad.msal4j;

class AuthenticationConstants {

    final static int AAD_JWT_TOKEN_LIFETIME_SECONDS = 60 * 10;
    final static String RESOURCE = "resource";

    static final String ID_TOKEN_SUBJECT = "sub";
    static final String ID_TOKEN_TENANTID = "tid";
    static final String ID_TOKEN_UPN = "upn";
    static final String ID_TOKEN_GIVEN_NAME = "given_name";
    static final String ID_TOKEN_FAMILY_NAME = "family_name";
    static final String ID_TOKEN_UNIQUE_NAME = "unique_name";
    static final String ID_TOKEN_EMAIL = "email";
    static final String ID_TOKEN_IDENTITY_PROVIDER = "idp";
    static final String ID_TOKEN_OBJECT_ID = "oid";
    static final String ID_TOKEN_PASSWORD_CHANGE_URL = "pwd_url";
    static final String ID_TOKEN_PASSWORD_EXPIRES_ON = "pwd_exp";
}
