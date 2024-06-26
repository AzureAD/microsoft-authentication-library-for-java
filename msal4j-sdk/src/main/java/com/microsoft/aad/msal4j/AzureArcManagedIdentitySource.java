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
import java.util.Collections;
import java.util.HashMap;

class AzureArcManagedIdentitySource extends AbstractManagedIdentitySource{

    private static final Logger LOG = LoggerFactory.getLogger(AzureArcManagedIdentitySource.class);
    private static final String ARC_API_VERSION = "2019-11-01";
    private static final String AZURE_ARC = "Azure Arc";
    private static final String WINDOWS_PATH = System.getenv("ProgramData") + "/AzureConnectedMachineAgent/Tokens/";
    private static final String LINUX_PATH = "/var/opt/azcmagent/tokens/";
    private static final String FILE_EXTENSION = ".key";
    private static final int MAX_FILE_SIZE_BYTES = 4096;

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

        LOG.info(String.format("[Managed Identity] Creating Azure Arc managed identity. Endpoint URI: %s", endpointUri));
        return endpointUri;
    }

    private AzureArcManagedIdentitySource(URI endpoint, MsalRequest msalRequest, ServiceBundle serviceBundle){
        super(msalRequest, serviceBundle, ManagedIdentitySourceType.AZURE_ARC);
        this.MSI_ENDPOINT = endpoint;

        ManagedIdentityIdType idType =
                ((ManagedIdentityApplication) msalRequest.application()).getManagedIdentityId().getIdType();
        if (idType != ManagedIdentityIdType.SYSTEM_ASSIGNED) {
            throw new MsalServiceException(String.format(MsalErrorMessage.MANAGED_IDENTITY_USER_ASSIGNED_NOT_SUPPORTED, AZURE_ARC), MsalError.USER_ASSIGNED_MANAGED_IDENTITY_NOT_SUPPORTED,
                    ManagedIdentitySourceType.AZURE_ARC);
        }
    }

    @Override
    public void createManagedIdentityRequest(String resource)
    {
        managedIdentityRequest.baseEndpoint = MSI_ENDPOINT;
        managedIdentityRequest.method = HttpMethod.GET;

        managedIdentityRequest.headers = new HashMap<>();
        managedIdentityRequest.headers.put("Metadata", "true");

        managedIdentityRequest.queryParameters = new HashMap<>();
        managedIdentityRequest.queryParameters.put("api-version", Collections.singletonList(ARC_API_VERSION));
        managedIdentityRequest.queryParameters.put("resource", Collections.singletonList(resource));
    }

    @Override
    public ManagedIdentityResponse handleResponse(
            ManagedIdentityParameters parameters,
            IHttpResponse response) {

        LOG.info("[Managed Identity] Response received. Status code: {response.StatusCode}");

        if (response.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            if(!response.headers().containsKey("WWW-Authenticate")) {
                LOG.error("[Managed Identity] WWW-Authenticate header is expected but not found.");
                throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_NO_CHALLENGE_ERROR, MsalError.MANAGED_IDENTITY_REQUEST_FAILED,
                        ManagedIdentitySourceType.AZURE_ARC);
            }

            String challenge = response.headers().get("WWW-Authenticate").get(0);
            String[] splitChallenge = challenge.split("=");

            if (splitChallenge.length != 2) {
                LOG.error("[Managed Identity] The WWW-Authenticate header for Azure arc managed identity is not an expected format.");
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

            return  super.handleResponse(parameters, response);
        }

        return super.handleResponse(parameters, response);
    }

    private void validateFile(Path path) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!(osName.contains("windows") || osName.contains("linux"))) {
            LOG.error(String.format("[Managed Identity] Unsupported platform: %s", osName));
            throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_PLATFORM_NOT_SUPPORTED, MsalError.MANAGED_IDENTITY_FILE_READ_ERROR,
                    ManagedIdentitySourceType.AZURE_ARC);
        }

        if (isValidWindowsPath(path) || isValidLinuxPath(path)) {
            if (path.toFile().length() > MAX_FILE_SIZE_BYTES) {
                LOG.error(String.format("[Managed Identity] File is larger than %s bytes.", MAX_FILE_SIZE_BYTES));
                throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_INVALID_FILEPATH, MsalError.MANAGED_IDENTITY_FILE_READ_ERROR,
                        ManagedIdentitySourceType.AZURE_ARC);
            }
        } else {
            LOG.error("[Managed Identity] Invalid filepath.");
            throw new MsalServiceException(MsalErrorMessage.MANAGED_IDENTITY_INVALID_FILEPATH, MsalError.MANAGED_IDENTITY_FILE_READ_ERROR,
                    ManagedIdentitySourceType.AZURE_ARC);
        }

        LOG.error("[Managed Identity] Path passed validation.");
    }

    private boolean isValidWindowsPath(Path path) {
        return path.startsWith(WINDOWS_PATH) && path.toString().toLowerCase().endsWith(FILE_EXTENSION);
    }

    private boolean isValidLinuxPath(Path path) {
        return path.startsWith(LINUX_PATH) && path.toString().toLowerCase().endsWith(FILE_EXTENSION);
    }
}