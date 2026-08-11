// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class AzureArcManagedIdentitySource extends AbstractManagedIdentitySource{

    private static final Logger LOG = LoggerFactory.getLogger(AzureArcManagedIdentitySource.class);
    private static final String ARC_API_VERSION = "2020-06-01";
    private static final String AZURE_ARC = "Azure Arc";
    private static final String WINDOWS_PATH = System.getenv("ProgramData") + "/AzureConnectedMachineAgent/Tokens/";
    private static final String LINUX_PATH = "/var/opt/azcmagent/tokens/";
    private static final String FILE_EXTENSION = ".key";
    private static final int MAX_FILE_SIZE_BYTES = 4096;
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    private final URI MSI_ENDPOINT;

    static AbstractManagedIdentitySource create(MsalRequest msalRequest, ServiceBundle serviceBundle)
    {
        IEnvironmentVariables environmentVariables = getEnvironmentVariables();
        String identityEndpoint = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_ENDPOINT);
        String imdsEndpoint = environmentVariables.getEnvironmentVariable(Constants.IMDS_ENDPOINT);

        URI validatedUri = validateAndGetUri(identityEndpoint, imdsEndpoint);
        return validatedUri == null ? null : new AzureArcManagedIdentitySource(validatedUri, msalRequest, serviceBundle );
    }

    private static URI validateAndGetUri(String identityEndpoint, String imdsEndpoint) {

        // if BOTH the env vars IDENTITY_ENDPOINT and IMDS_ENDPOINT are set the MsiType is Azure Arc
        if (StringHelper.isNullOrBlank(identityEndpoint) || StringHelper.isNullOrBlank(imdsEndpoint))
        {
            LOG.info("[Managed Identity] Azure Arc managed identity is unavailable.");
            return null;
        }

        URI endpointUri;
        try {
            endpointUri = new URI(identityEndpoint);
        } catch (URISyntaxException e) {
            throw new MsalServiceException(String.format(
                    MsalErrorMessage.MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR, "IDENTITY_ENDPOINT", identityEndpoint, AZURE_ARC), MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT,
                    ManagedIdentitySourceType.AZURE_ARC);
        }

        LOG.info("[Managed Identity] Creating Azure Arc managed identity. Endpoint URI: {}", endpointUri);
        return endpointUri;
    }

    private AzureArcManagedIdentitySource(URI endpoint, MsalRequest msalRequest, ServiceBundle serviceBundle){
        super(msalRequest, serviceBundle, ManagedIdentitySourceType.AZURE_ARC);
        this.MSI_ENDPOINT = endpoint;
    }

    @Override
    public void createManagedIdentityRequest(String resource)
    {
        managedIdentityRequest.baseEndpoint = MSI_ENDPOINT;
        managedIdentityRequest.method = HttpMethod.GET;

        managedIdentityRequest.headers = new HashMap<>();
        managedIdentityRequest.headers.put("Metadata", "true");

        managedIdentityRequest.queryParameters = new HashMap<>();
        managedIdentityRequest.queryParameters.put("api-version", ARC_API_VERSION);
        managedIdentityRequest.queryParameters.put("resource", resource);

        if (this.idType != null && this.idType != ManagedIdentityIdType.SYSTEM_ASSIGNED
                && !StringHelper.isNullOrBlank(this.userAssignedId)) {
            LOG.info("[Managed Identity] Adding user assigned ID to the request for Azure Arc Managed Identity.");
            managedIdentityRequest.addUserAssignedIdToQuery(this.idType, this.userAssignedId);
        }
    }

    @Override
    public ManagedIdentityResponse handleResponse(
            ManagedIdentityParameters parameters,
            IHttpResponse response) {

        LOG.info("[Managed Identity] Response received. Status code: {}", response.statusCode());

        if (response.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            String challenge =
                    readChallengeFrom(response)
                            .orElseGet(() -> {
                                LOG.error("[Managed Identity] {} is expected but not found.", WWW_AUTHENTICATE_HEADER);
                                throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_NO_CHALLENGE_ERROR, MsalError.MANAGED_IDENTITY_REQUEST_FAILED,
                                        ManagedIdentitySourceType.AZURE_ARC);
                            });
            String[] splitChallenge = challenge.split("=");

            if (splitChallenge.length != 2) {
                LOG.error("[Managed Identity] The {} header for Azure arc managed identity is not an expected format.", WWW_AUTHENTICATE_HEADER);
                throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_INVALID_CHALLENGE, MsalError.MANAGED_IDENTITY_REQUEST_FAILED,
                        ManagedIdentitySourceType.AZURE_ARC);
            }

            Path path = Paths.get(splitChallenge[1]).normalize();

            validateFile(path);

            if (!path.toFile().exists()) {
                LOG.error("[Managed Identity] The WWW-Authenticate header specifies a file that does not exist");
                throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_INVALID_FILEPATH, MsalError.MANAGED_IDENTITY_FILE_READ_ERROR,
                        ManagedIdentitySourceType.AZURE_ARC);
            }

            String authHeaderValue = null;
            try {
                authHeaderValue = "Basic " + new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new MsalServiceException(e.getMessage(), MsalError.MANAGED_IDENTITY_FILE_READ_ERROR, ManagedIdentitySourceType.AZURE_ARC);
            }

            createManagedIdentityRequest(parameters.resource);

            LOG.info("[Managed Identity] Adding authorization header to the request.");

            managedIdentityRequest.headers.put("Authorization", authHeaderValue);

            try {
                response = serviceBundle.getHttpHelper().executeHttpRequest(
                        new HttpRequest(HttpMethod.GET, managedIdentityRequest.computeURI().toString(),
                                managedIdentityRequest.headers),
                        managedIdentityRequest.requestContext(),
                        serviceBundle);
            } catch (URISyntaxException e) {
                throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR, MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT,
                        managedIdentitySourceType);
            }

            ManagedIdentityResponse tokenResponse = super.handleResponse(parameters, response);
            verifyUserAssignedIdentityWasHonored(tokenResponse);
            return tokenResponse;
        }

        ManagedIdentityResponse tokenResponse = super.handleResponse(parameters, response);
        verifyUserAssignedIdentityWasHonored(tokenResponse);
        return tokenResponse;
    }

    // verifyUserAssignedIdentityWasHonored fails closed when a user-assigned identity was requested but
    // Azure Arc did not confirm it in the token response. A legacy Azure Arc agent ignores the
    // client_id / msi_res_id / object_id selector and silently returns the machine's system-assigned
    // identity. An agent that supports user-assigned managed identity echoes the identity it used in the
    // token response; when that echo is missing or does not match the requested selector, MSAL must not
    // hand back a token for a different identity than the one requested.
    private void verifyUserAssignedIdentityWasHonored(ManagedIdentityResponse response) {
        if (idType == null || idType == ManagedIdentityIdType.SYSTEM_ASSIGNED
                || StringHelper.isNullOrBlank(userAssignedId)) {
            // System-assigned: there is no requested identity to confirm.
            return;
        }

        String echoed;
        switch (idType) {
            case CLIENT_ID:
                echoed = response.getClientId();
                break;
            case OBJECT_ID:
                echoed = response.getObjectId();
                break;
            case RESOURCE_ID: {
                // Azure Arc returns the resource id under msi_res_id; mi_res_id is accepted as a
                // safety net. If both aliases are present and disagree, the echo cannot be trusted,
                // so fail closed rather than accept one alias that matches while the other contradicts it.
                String msiResId = response.getMsiResId();
                String miResId = response.getMiResId();
                if (!StringHelper.isNullOrBlank(msiResId) && !StringHelper.isNullOrBlank(miResId)
                        && !msiResId.equalsIgnoreCase(miResId)) {
                    echoed = null;
                } else {
                    echoed = !StringHelper.isNullOrBlank(msiResId) ? msiResId : miResId;
                }
                break;
            }
            default:
                echoed = null;
                break;
        }

        // Compare case-insensitively: client_id / object_id are GUIDs, and an ARM resource id
        // (msi_res_id) can legitimately differ in segment casing.
        if (StringHelper.isNullOrBlank(echoed) || !echoed.equalsIgnoreCase(userAssignedId)) {
            LOG.error("[Managed Identity] Azure Arc did not confirm the requested user-assigned managed identity in the token response.");
            throw new MsalServiceException(String.format(MsalErrorMessage.MANAGED_IDENTITY_USER_ASSIGNED_NOT_CONFIRMED, AZURE_ARC),
                    MsalError.USER_ASSIGNED_MANAGED_IDENTITY_NOT_CONFIRMED, ManagedIdentitySourceType.AZURE_ARC);
        }
    }

    private Optional<String> readChallengeFrom(IHttpResponse response) {
        return response.headers()
                .entrySet()
                .stream()
                .filter(entry -> WWW_AUTHENTICATE_HEADER.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .findFirst();
    }

    private void validateFile(Path path) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!(osName.contains("windows") || osName.contains("linux"))) {
            LOG.error("[Managed Identity] Unsupported platform: {}", osName);
            throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_PLATFORM_NOT_SUPPORTED, MsalError.MANAGED_IDENTITY_FILE_READ_ERROR,
                    ManagedIdentitySourceType.AZURE_ARC);
        }

        if (isValidWindowsPath(path) || isValidLinuxPath(path)) {
            if (path.toFile().length() > MAX_FILE_SIZE_BYTES) {
                LOG.error("[Managed Identity] File is larger than {} bytes.", MAX_FILE_SIZE_BYTES);
                throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_INVALID_FILEPATH, MsalError.MANAGED_IDENTITY_FILE_READ_ERROR,
                        ManagedIdentitySourceType.AZURE_ARC);
            }
        } else {
            LOG.error("[Managed Identity] Invalid filepath.");
            throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_INVALID_FILEPATH, MsalError.MANAGED_IDENTITY_FILE_READ_ERROR,
                    ManagedIdentitySourceType.AZURE_ARC);
        }

        LOG.info("[Managed Identity] Path passed validation.");
    }

    private boolean isValidWindowsPath(Path path) {
        return path.startsWith(WINDOWS_PATH) && path.toString().toLowerCase().endsWith(FILE_EXTENSION);
    }

    private boolean isValidLinuxPath(Path path) {
        return path.startsWith(LINUX_PATH) && path.toString().toLowerCase().endsWith(FILE_EXTENSION);
    }
}