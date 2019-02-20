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

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

abstract class AuthenticationResultSupplier implements Supplier<AuthenticationResult> {

    ClientDataHttpHeaders headers;
    ClientApplicationBase clientApplication;

    AuthenticationResultSupplier(ClientApplicationBase clientApplication) {
        this.clientApplication = clientApplication;
    }

    abstract AuthenticationResult execute() throws Exception;

    @Override
    public AuthenticationResult get() {
        AuthenticationResult result;
        try {
            result = execute();

            logResult(result, headers);
        } catch (Exception ex) {
            clientApplication.log.error(
                    LogHelper.createMessage("Execution of " + this.getClass() + " failed.",
                            this.headers.getHeaderCorrelationIdValue()), ex);

            throw new CompletionException(ex);
        }
        return result;
    }

    void logResult(AuthenticationResult result, ClientDataHttpHeaders headers)
    {
        if (!StringHelper.isBlank(result. getAccessToken())) {

            String accessTokenHash = this.computeSha256Hash(result
                    .getAccessToken());
            if (!StringHelper.isBlank(result.getRefreshToken())) {
                String refreshTokenHash = this.computeSha256Hash(result
                        .getRefreshToken());
                if(clientApplication.isLogPii()){
                    clientApplication.log.debug(LogHelper.createMessage(String
                                    .format("Access Token with hash '%s' and Refresh Token with hash '%s' returned",
                                            accessTokenHash, refreshTokenHash),
                            headers.getHeaderCorrelationIdValue()));
                }
                else{
                    clientApplication.log.debug(
                            LogHelper.createMessage("Access Token and Refresh Token were returned",
                            headers.getHeaderCorrelationIdValue()));
                }
            }
            else {
                if(clientApplication.isLogPii()){
                    clientApplication.log.debug(LogHelper.createMessage(String
                                    .format("Access Token with hash '%s' returned",
                                            accessTokenHash),
                            headers.getHeaderCorrelationIdValue()));
                }
                else{
                    clientApplication.log.debug(LogHelper.createMessage("Access Token was returned",
                            headers.getHeaderCorrelationIdValue()));
                }
            }
        }
    }

    private String computeSha256Hash(String input) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(input.getBytes("UTF-8"));
            byte[] hash = digest.digest();
            return Base64.encodeBase64URLSafeString(hash);
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException ex){
            clientApplication.log.warn(
                    LogHelper.createMessage("Failed to compute SHA-256 hash due to exception - ",
                            LogHelper.getPiiScrubbedDetails(ex)));
            return "Failed to compute SHA-256 hash";
        }
    }
}
