// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

abstract class AuthenticationResultSupplier implements Supplier<IAuthenticationResult> {

    AbstractClientApplicationBase clientApplication;
    MsalRequest msalRequest;

    AuthenticationResultSupplier(AbstractClientApplicationBase clientApplication, MsalRequest msalRequest) {
        this.clientApplication = clientApplication;
        this.msalRequest = msalRequest;
    }

    Authority getAuthorityWithPrefNetworkHost(String authority) throws Exception {

        URL authorityUrl = new URL(authority);

        InstanceDiscoveryMetadataEntry discoveryMetadataEntry =
                AadInstanceDiscoveryProvider.getMetadataEntry(
                        authorityUrl,
                        clientApplication.validateAuthority(),
                        msalRequest,
                        clientApplication.getServiceBundle());

        URL updatedAuthorityUrl =
                new URL(authorityUrl.getProtocol(), discoveryMetadataEntry.preferredNetwork, authorityUrl.getFile());

        return Authority.createAuthority(updatedAuthorityUrl);
    }

    abstract AuthenticationResult execute() throws Exception;

    @Override
    public IAuthenticationResult get() {
        AuthenticationResult result;

        ApiEvent apiEvent = initializeApiEvent(msalRequest);

        try(TelemetryHelper telemetryHelper =
                    clientApplication.getServiceBundle().getTelemetryManager().createTelemetryHelper(
                            msalRequest.requestContext().telemetryRequestId(),
                            msalRequest.application().clientId(),
                            apiEvent,
                            true)) {
            try {
                result = execute();
                apiEvent.setWasSuccessful(true);

                if(result != null){
                    logResult(result, msalRequest.headers());

                    if (result.account() != null) {
                        apiEvent.setTenantId(result.accountCacheEntity().realm());
                    }
                }
            } catch(Exception ex) {

                String error = StringHelper.EMPTY_STRING;
                if (ex instanceof MsalException) {
                    MsalException exception = ((MsalException) ex);
                    if(exception.errorCode() != null){
                        apiEvent.setApiErrorCode(exception.errorCode());
                    }
                } else {
                    if(ex.getCause() != null){
                        error = ex.getCause().toString();
                    }
                }

                clientApplication.getServiceBundle().getServerSideTelemetry().addFailedRequestTelemetry(
                        String.valueOf(msalRequest.requestContext().publicApi().getApiId()),
                        msalRequest.requestContext().correlationId(),
                        error);

                clientApplication.log.error(
                        LogHelper.createMessage(
                                "Execution of " + this.getClass() + " failed.",
                                msalRequest.headers().getHeaderCorrelationIdValue()), ex);

                throw new CompletionException(ex);
            }
        }
        return result;
    }

    private void logResult(AuthenticationResult result, HttpHeaders headers) {
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
        msalRequest.requestContext().telemetryRequestId(
                clientApplication.getServiceBundle().getTelemetryManager().generateRequestId());
        apiEvent.setApiId(msalRequest.requestContext().publicApi().getApiId());
        apiEvent.setCorrelationId(msalRequest.requestContext().correlationId());
        apiEvent.setRequestId(msalRequest.requestContext().telemetryRequestId());
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
            return Base64.getUrlEncoder().encodeToString(hash);
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException ex){
            clientApplication.log.warn(LogHelper.createMessage(
                    "Failed to compute SHA-256 hash due to exception - ",
                    LogHelper.getPiiScrubbedDetails(ex)));
            return "Failed to compute SHA-256 hash";
        }
    }
}