// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;


import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

abstract class AuthenticationResultSupplier implements Supplier<IAuthenticationResult> {

    AbstractApplicationBase clientApplication;
    MsalRequest msalRequest;

    AuthenticationResultSupplier(AbstractApplicationBase clientApplication, MsalRequest msalRequest) {
        this.clientApplication = clientApplication;
        this.msalRequest = msalRequest;
    }

    Authority getAuthorityWithPrefNetworkHost(String authority) throws MalformedURLException {

        URL authorityUrl = new URL(authority);

        if (msalRequest.requestContext().apiParameters().tenant() != null) {
            authorityUrl = new URL(authority.replace(
                    Authority.getTenant(authorityUrl, Authority.detectAuthorityType(authorityUrl)),
                    msalRequest.requestContext().apiParameters().tenant()));
        }

        InstanceDiscoveryMetadataEntry discoveryMetadataEntry =
                AadInstanceDiscoveryProvider.getMetadataEntry(
                        authorityUrl,
                        clientApplication.validateAuthority(),
                        msalRequest,
                        clientApplication.serviceBundle());

        URL updatedAuthorityUrl = new URL(
                authorityUrl.getProtocol(),
                discoveryMetadataEntry.preferredNetwork,
                authorityUrl.getPort(),
                authorityUrl.getFile());

        return Authority.createAuthority(updatedAuthorityUrl);
    }

    abstract AuthenticationResult execute() throws Exception;

    @Override
    public IAuthenticationResult get() {
        AuthenticationResult result;

        ApiEvent apiEvent = initializeApiEvent(msalRequest);

        try (TelemetryHelper telemetryHelper =
                     clientApplication.serviceBundle().getTelemetryManager().createTelemetryHelper(
                             msalRequest.requestContext().telemetryRequestId(),
                             msalRequest.application().clientId(),
                             apiEvent,
                             true)) {
            try {
                result = execute();
                apiEvent.setWasSuccessful(true);

                if (result != null) {
                    logResult(result, msalRequest.headers());

                    if (result.account() != null) {
                        apiEvent.setTenantId(result.accountCacheEntity().realm());
                    }
                }
            } catch (Exception ex) {

                String error = StringHelper.EMPTY_STRING;
                if (ex instanceof MsalException) {
                    MsalException exception = ((MsalException) ex);
                    if (exception.errorCode() != null) {
                        apiEvent.setApiErrorCode(exception.errorCode());
                    }
                } else {
                    if (ex.getCause() != null) {
                        error = ex.getCause().toString();
                    }
                }

                clientApplication.serviceBundle().getServerSideTelemetry().addFailedRequestTelemetry(
                        String.valueOf(msalRequest.requestContext().publicApi().getApiId()),
                        msalRequest.requestContext().correlationId(),
                        error);

                logException(ex);
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
                if (clientApplication.logPii()) {
                    clientApplication.log.debug(LogHelper.createMessage(String.format(
                                    "Access Token with hash '%s' and Refresh Token with hash '%s' returned",
                                    accessTokenHash, refreshTokenHash),
                            headers.getHeaderCorrelationIdValue()));
                } else {
                    clientApplication.log.debug(
                            LogHelper.createMessage(
                                    "Access Token and Refresh Token were returned",
                                    headers.getHeaderCorrelationIdValue()));
                }
            } else {
                if (clientApplication.logPii()) {
                    clientApplication.log.debug(LogHelper.createMessage(String.format(
                                    "Access Token with hash '%s' returned", accessTokenHash),
                            headers.getHeaderCorrelationIdValue()));
                } else {
                    clientApplication.log.debug(LogHelper.createMessage(
                            "Access Token was returned",
                            headers.getHeaderCorrelationIdValue()));
                }
            }
        }
    }

    private void logException(Exception ex) {

        String logMessage = LogHelper.createMessage(
                "Execution of " + this.getClass() + " failed.",
                msalRequest.headers().getHeaderCorrelationIdValue());

        if (ex instanceof MsalClientException) {
            MsalClientException exception = (MsalClientException) ex;
            if (exception.errorCode() != null && exception.errorCode().equalsIgnoreCase(AuthenticationErrorCode.CACHE_MISS)) {
                clientApplication.log.debug(logMessage, ex);
                return;
            }
        } else if (ex instanceof MsalAzureSDKException) {
            clientApplication.log.debug(ex.getMessage(), ex);
            return;
        }

        clientApplication.log.error(logMessage, ex);
    }

    private ApiEvent initializeApiEvent(MsalRequest msalRequest) {
        ApiEvent apiEvent = new ApiEvent(clientApplication.logPii());
        msalRequest.requestContext().telemetryRequestId(
                clientApplication.serviceBundle().getTelemetryManager().generateRequestId());
        apiEvent.setApiId(msalRequest.requestContext().publicApi().getApiId());
        apiEvent.setCorrelationId(msalRequest.requestContext().correlationId());
        apiEvent.setRequestId(msalRequest.requestContext().telemetryRequestId());
        apiEvent.setWasSuccessful(false);

        apiEvent.setIsConfidentialClient(clientApplication instanceof ConfidentialClientApplication);

        try {
            Authority authenticationAuthority = clientApplication.authenticationAuthority;
            if (authenticationAuthority != null) {
                apiEvent.setAuthority(new URI(authenticationAuthority.authority()));
                apiEvent.setAuthorityType(authenticationAuthority.authorityType().toString());
            }
        } catch (URISyntaxException ex) {
            clientApplication.log.warn(LogHelper.createMessage(
                    "Setting URL telemetry fields failed: " +
                            LogHelper.getPiiScrubbedDetails(ex),
                    msalRequest.headers().getHeaderCorrelationIdValue()));
        }

        return apiEvent;
    }

    private String computeSha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(input.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();
            return Base64.getUrlEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            clientApplication.log.warn(LogHelper.createMessage(
                    "Failed to compute SHA-256 hash due to exception - ",
                    LogHelper.getPiiScrubbedDetails(ex)));
            return "Failed to compute SHA-256 hash";
        }
    }
}