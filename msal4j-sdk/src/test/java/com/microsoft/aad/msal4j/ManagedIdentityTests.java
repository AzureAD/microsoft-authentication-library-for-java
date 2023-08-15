// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.core.util.UrlBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class ManagedIdentityTests {

    static final String resource = "https://management.azure.com";
    private final static String resourceDefaultSuffix = "https://management.azure.com/.default";
    final static String appServiceEndpoint = "http://127.0.0.1:41564/msi/token";
    final static String IMDS_ENDPOINT = "http://169.254.169.254/metadata/identity/oauth2/token";
    final static String azureArcEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";
    final static String cloudShellEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";
    final static String serviceFabricEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";

    private String getResponse(String resource) {
        long expiresOn = Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();
        String response = "{\"access_token\":\"accesstoken\",\"expires_on\":\"" + expiresOn + "\",\"resource\":\"" + resource + "\",\"token_type\":" +
                "\"Bearer\",\"client_id\":\"client_id\"}";

        return response;
    }

    private HttpRequest expectedRequest(ManagedIdentitySourceType source, String resource) throws URISyntaxException {
        HttpRequest request;
        Map<String, String> headers = new HashMap<>();

        switch (source) {
            case AppService: {
                String endpoint = appServiceEndpoint + "?api-version=2019-08-01&resource=" + resource;
                headers.put("X-IDENTITY-HEADER", "secret");
                return new HttpRequest(HttpMethod.GET, endpoint, headers);
            }
            case Imds: {
                String endpoint = IMDS_ENDPOINT + "?api-version=2018-02-01&resource=" + resource;
                headers.put("Metadata", "true");
                return new HttpRequest(HttpMethod.GET, endpoint, headers);
            }
        }

        return null;
    }

    private HttpResponse expectedResponse(int statusCode, String response) {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.statusCode(statusCode);
        httpResponse.body(response);

        return httpResponse;
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.ManagedIdentityTestDataProvider#createData")
    void managedIdentityTest_SuccessfulResponse(ManagedIdentitySourceType source, String endpoint, String resource) throws Exception {
        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(source, endpoint);
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        lenient().when(httpClientMock.send(eq(expectedRequest(source, resource)))).thenReturn(expectedResponse(200, getResponse(resource)));

        ManagedIdentityApplication miApp = ManagedIdentityApplication
                .builder(ManagedIdentityId.SystemAssigned())
                .httpClient(httpClientMock)
                .build();

        IAuthenticationResult result = miApp.acquireTokenForManagedIdentity(
                ManagedIdentityParameters.builder(resource)
                        .environmentVariables(environmentVariables)
                        .build()).get();

        assertNotNull(result.accessToken());

        String accessToken = result.accessToken();

        result = miApp.acquireTokenForManagedIdentity(
                ManagedIdentityParameters.builder(resource)
                        .environmentVariables(environmentVariables)
                        .build()).get();

        assertNotNull(result.accessToken());
        assertEquals(accessToken, result.accessToken());
    }
}
