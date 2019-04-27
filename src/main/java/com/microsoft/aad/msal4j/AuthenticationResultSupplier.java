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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

abstract class AuthenticationResultSupplier implements Supplier<AuthenticationResult> {

    ClientApplicationBase clientApplication;
    MsalRequest msalRequest;

    AuthenticationResultSupplier(ClientApplicationBase clientApplication, MsalRequest msalRequest) {
        this.clientApplication = clientApplication;
        this.msalRequest = msalRequest;
    }

    Authority getAuthorityWithPrefNetworkHost(String authority) throws Exception {

        URL authorityUrl = new URL(authority);

        InstanceDiscoveryMetadataEntry discoveryMetadataEntry =
                AadInstanceDiscovery.GetMetadataEntry
                        (authorityUrl, clientApplication.validateAuthority(), msalRequest,
                                clientApplication.getServiceBundle());

        URL updatedAuthorityUrl =
                new URL(authorityUrl.getProtocol(), discoveryMetadataEntry.preferredNetwork, authorityUrl.getFile());

        return Authority.createAuthority(updatedAuthorityUrl);
    }

    abstract AuthenticationResult execute() throws Exception;

    @Override
    public AuthenticationResult get() {
        AuthenticationResult result;

        ApiEvent apiEvent = initializeApiEvent(msalRequest);

        try(TelemetryHelper telemetryHelper =
                    clientApplication.getServiceBundle().getTelemetryManager().createTelemetryHelper(
                            msalRequest.requestContext().getTelemetryRequestId(),
                            msalRequest.application().clientId(),
                            apiEvent,
                            true)) {
            try {
                result = execute();
                logResult(result, msalRequest.headers());

                apiEvent.setWasSuccessful(true);
                if (result.account() != null) {
                    apiEvent.setTenantId(result.account().realm());
                }
            } catch(Exception ex) {
                if (ex instanceof AuthenticationException) {
                    apiEvent.setApiErrorCode(((AuthenticationException) ex).getErrorCode());
                }
                clientApplication.log.error(
                        LogHelper.createMessage(
                                "Execution of " + this.getClass() + " failed.",
                                msalRequest.headers().getHeaderCorrelationIdValue()), ex);

                throw new CompletionException(ex);
            }
        }
        return result;
    }

    void logResult(AuthenticationResult result, ClientDataHttpHeaders headers)
    {
        if (!StringHelper.isBlank(result.accessToken())) {

            String accessTokenHash = this.computeSha256Hash(result
                    .accessToken());
            if (!StringHelper.isBlank(result.refreshToken())) {
                String refreshTokenHash = this.computeSha256Hash(result
                        .refreshToken());
                if(clientApplication.logPii()){
                    clientApplication.log.debug(LogHelper.createMessage(String.format(
                            "Access Token with hash '%s' and Refresh Token with hash '%s' returned",
                            accessTokenHash, refreshTokenHash),
                            headers.getHeaderCorrelationIdValue()));
                }
                else{
                    clientApplication.log.debug(
                            LogHelper.createMessage(
                                    "Access Token and Refresh Token were returned",
                                    headers.getHeaderCorrelationIdValue()));
                }
            }
            else {
                if(clientApplication.logPii()){
                    clientApplication.log.debug(LogHelper.createMessage(String.format(
                            "Access Token with hash '%s' returned", accessTokenHash),
                            headers.getHeaderCorrelationIdValue()));
                }
                else{
                    clientApplication.log.debug(LogHelper.createMessage(
                            "Access Token was returned",
                            headers.getHeaderCorrelationIdValue()));
                }
            }
        }
    }

    private ApiEvent initializeApiEvent(MsalRequest msalRequest){
        ApiEvent apiEvent = new ApiEvent(clientApplication.logPii());
        msalRequest.requestContext().setTelemetryRequestId(
                clientApplication.getServiceBundle().getTelemetryManager().generateRequestId());
        apiEvent.setApiId(msalRequest.requestContext().getAcquireTokenPublicApi().getApiId());
        apiEvent.setCorrelationId(msalRequest.requestContext().getCorrelationId());
        apiEvent.setRequestId(msalRequest.requestContext().getTelemetryRequestId());
        apiEvent.setWasSuccessful(false);

        if(clientApplication instanceof ConfidentialClientApplication){
            apiEvent.setIsConfidentialClient(true);
        } else {
            apiEvent.setIsConfidentialClient(false);
        }

        try {
            Authority authenticationAuthority = clientApplication.authenticationAuthority;
            if (authenticationAuthority != null) {
                apiEvent.setAuthority(new URI(authenticationAuthority.authority()));
                apiEvent.setAuthorityType(authenticationAuthority.authorityType().toString());
            }
        } catch (URISyntaxException ex){
            clientApplication.log.warn(LogHelper.createMessage(
                    "Setting URL telemetry fields failed: " +
                            LogHelper.getPiiScrubbedDetails(ex),
                    msalRequest.headers().getHeaderCorrelationIdValue()));
        }

        return apiEvent;
    }

    private String computeSha256Hash(String input) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(input.getBytes("UTF-8"));
            byte[] hash = digest.digest();
            return Base64.encodeBase64URLSafeString(hash);
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException ex){
            clientApplication.log.warn(LogHelper.createMessage(
                    "Failed to compute SHA-256 hash due to exception - ",
                    LogHelper.getPiiScrubbedDetails(ex)));
            return "Failed to compute SHA-256 hash";
        }
    }
}