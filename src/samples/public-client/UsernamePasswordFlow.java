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

import com.microsoft.aad.msal4j.AuthenticationResult;
import com.microsoft.aad.msal4j.PublicClientApplication;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class UsernamePasswordFlow {

    public static void main(String args[]) throws Exception {
        getAccessTokenFromUserCredentials();
    }

    private static void getAccessTokenFromUserCredentials() throws Exception {
        PublicClientApplication app = new PublicClientApplication.Builder(TestData.PUBLIC_CLIENT_ID)
                .authority(TestData.AUTHORITY)
                .build();

        CompletableFuture<AuthenticationResult> future = app.acquireTokenByUsernamePassword
                (Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE), TestData.USER_NAME, TestData.USER_PASSWORD);

        future.handle((res, ex) -> {
            if(ex != null) {
                System.out.println("Oops! We have an exception - " + ex.getMessage());
                return "Unknown!";
            }
            System.out.println("Returned ok - " + res);

            System.out.println("Access Token - " + res.getAccessToken());
            System.out.println("Refresh Token - " + res.getRefreshToken());
            System.out.println("ID Token - " + res.getIdToken());
            return res;
        });

        future.join();
    }
}
