// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;


import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.apache.http.HttpStatus;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@PowerMockIgnore({"javax.net.ssl.*"})
@PrepareForTest({DefaultHttpClient.class})
public class ManagedIdentityTests extends AbstractMsalTests {

    final static String Resource = "https://management.azure.com";

    final static String ResourceDefaultSuffix = "https://management.azure.com/.default";
    final static String AppServiceEndpoint = "http://127.0.0.1:41564/msi/token";
    final static String IMDS_ENDPOINT = "http://169.254.169.254/metadata/identity/oauth2/token";
    final static String AzureArcEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";
    final static String CloudShellEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";
    final static String ServiceFabricEndpoint = "http://localhost:40342/metadata/identity/oauth2/token";

    private String getResponse() {
        long expiresOn = Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();
        String response = "{\"access_token\":\"accesstoken\",\"expires_on\":\"" + expiresOn + "\",\"resource\":\"https://management.azure.com/\",\"token_type\":" +
                "\"Bearer\",\"client_id\":\"client_id\"}";

        return response;
    }

    @Test
    public void managedIdentityHappyPathAsync() throws Exception {

        HttpResponse response = new HttpResponse();
        response.statusCode(200);
        response.body(getResponse());

        IEnvironmentVariables environmentVariables = new EnvironmentVariablesHelper(ManagedIdentitySourceType.AppService, AppServiceEndpoint);

        // Mock
        DefaultHttpClient defaultHttpClient = PowerMock.createMock(DefaultHttpClient.class);
        EasyMock.expect(defaultHttpClient.send((HttpRequest)EasyMock.anyObject())).andReturn(response).times(1);

        ManagedIdentityApplication.Builder miBuilder = ManagedIdentityApplication.builder(ManagedIdentityId.SystemAssigned())
                .httpClient(defaultHttpClient);

        ManagedIdentityApplication mi = miBuilder.build();

        PowerMock.replay(defaultHttpClient);

        // Execute
        IAuthenticationResult result = mi.acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(Resource)
                .environmentVariables(environmentVariables)
                .build()).get();

        // Assert
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());

        IAuthenticationResult resultFromCache = mi.acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(Resource)
                .environmentVariables(environmentVariables)
                .build()).get();

        Assert.assertNotNull(resultFromCache);
        Assert.assertEquals(result.accessToken(), resultFromCache.accessToken());
    }
}
