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

package lapapi;

public class LabConstants {
    public final static String MOBILE_DEVICE_MANAGEMENT_WITH_CONDITIONAL_ACCESS = "mdmca";
    public final static String MOBILE_APP_MANAGEMENT_WITH_CONDITIONAL_ACCESS = "mamca";
    public final static String MOBILE_APP_MANAGEMENT = "mam";
    public final static String MULTIFACTOR_AUTHENTICATION = "mfa";
    public final static String LICENSE = "license";
    public final static String FEDERATION_PROVIDER = "federationProvider";
    public final static String FEDERATED_USER = "isFederated";
    public final static String USERTYPE = "usertype";
    public final static String EXTERNAL = "external";
    public final static String B2C_PROVIDER = "b2cProvider";
    public final static String B2C_LOCAL = "local";
    public final static String B2C_FACEBOOK = "facebook";
    public final static String B2C_GOOGLE = "google";

    public final static String TRUE = "true";
    public final static String FALSE = "false";

    public final static String LAB_ENDPOINT = "https://api.msidlab.com/api/user";
    public final static String APP_ID_URL = "https://msidlabs.vault.azure.net/secrets/LabVaultAppID/4032a45f48dc424d8edd445a42d25960";
    public final static String APP_PASSWORD_URL = "https://msidlabs.vault.azure.net/secrets/LabVaultAppSecret/c2be68b1f01d4861819d6afde2ec71e3";
}