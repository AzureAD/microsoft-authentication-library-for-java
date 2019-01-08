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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.naming.ServiceUnavailableException;

import com.microsoft.aad.msal4j.AuthenticationContext;
import com.microsoft.aad.msal4j.AuthenticationResult;
import com.microsoft.aad.msal4j.DeviceCode;

public class PublicClient {

    private final static String AUTHORITY = "https://login.microsoftonline.com/common";
    private final static String CLIENT_ID = "YOUR_PUBLIC_CLIENT_ID";
    private final static String RESOURCE = "https://graph.windows.net";

    public static void main(String args[]) throws Exception {
            AuthenticationResult result = getAccessTokenUsingDeviceCodeFlow();
            System.out.println("Access Token - " + result.getAccessToken());
            System.out.println("Refresh Token - " + result.getRefreshToken());
            System.out.println("ID Token - " + result.getIdToken());
        }

    private static AuthenticationResult getAccessTokenUsingDeviceCodeFlow() throws Exception {
        AuthenticationContext context = null;
        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(AUTHORITY, true, service);

            Future<DeviceCode> future = context.acquireDeviceCode(CLIENT_ID, RESOURCE, null);
            DeviceCode deviceCode = future.get();
            System.out.println(deviceCode.getMessage());
            System.out.println("Press Enter after authenticating");
            System.in.read();
            Future<AuthenticationResult> futureResult = context.acquireTokenByDeviceCode(deviceCode, null);
            result = futureResult.get();
        } finally {
            service.shutdown();
        }
        if (result == null) {
            throw new ServiceUnavailableException("authentication result was null");
        }
        return result;
    }
}
