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

public class TestConstants {
    public final static String KEYVAULT_DEFAULT_SCOPE = "https://vault.azure.net/.default";
    public final static String GRAPH_DEFAULT_SCOPE = "https://graph.windows.net/.default";
    public final static String B2C_LAB_SCOPE = "https://msidlabb2c.onmicrosoft.com/msaapp/user_impersonation";

    public final static String MICROSOFT_AUTHORITY_HOST = "https://login.microsoftonline.com/";
    public final static String ORGANIZATIONS_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "organizations/";
    public final static String MICROSOFT_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "microsoft.onmicrosoft.com";

    public final static String B2C_AUTHORITY = "https://msidlabb2c.b2clogin.com/tfp/msidlabb2c.onmicrosoft.com/";
    public final static String B2C_AUTHORITY_URL = "https://msidlabb2c.b2clogin.com/msidlabb2c.onmicrosoft.com/";
    public final static String B2C_ROPC_POLICY = "B2C_1_ROPC_Auth";
    public final static String B2C_SIGN_IN_POLICY = "B2C_1_SignInPolicy";
    public final static String B2C_AUTHORITY_SIGN_IN = B2C_AUTHORITY + B2C_SIGN_IN_POLICY;
    public final static String B2C_AUTHORITY_ROPC = B2C_AUTHORITY + B2C_ROPC_POLICY;
    public final static String B2C_READ_SCOPE = "https://msidlabb2c.onmicrosoft.com/msidlabb2capi/read";
    public final static String B2C_MICROSOFTLOGIN_AUTHORITY = "https://login.microsoftonline.com/tfp/msidlabb2c.onmicrosoft.com/";
    public final static String B2C_MICROSOFTLOGIN_ROPC = B2C_MICROSOFTLOGIN_AUTHORITY + B2C_ROPC_POLICY;

    public final static String LOCALHOST = "http://localhost:";
    public final static String LOCAL_FLAG_ENV_VAR = "MSAL_JAVA_RUN_LOCAL";
}
