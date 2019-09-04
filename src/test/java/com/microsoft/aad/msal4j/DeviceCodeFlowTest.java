// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.TestConfiguration.*;


@Test(groups = { "checkin" })
@PrepareForTest({HttpHelper.class, PublicClientApplication.class })
public class DeviceCodeFlowTest extends PowerMockTestCase {
    private PublicClientApplication app = null;

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }

    public static Map<String, String> getQueryMap(String query)
    {
        Map<String, String> map = new HashMap<>();
        for (String param : query.split("&"))
        {
            map.put(param.split("=")[0], param.split("=")[1]);
        }
        return map;
    }

    String deviceCodeJsonResponse = "{\n" +
            "  \"user_code\": \"DW83JNP2P\",\n" +
            "  \"device_code\": \"DAQABAAEAAADRNYRQ3dhRFEeqWvq-yi6QodK2pb1iAA\",\n" +
            "  \"verification_uri\": \"https://aka.ms/devicelogin\",\n" +
            "  \"expires_in\": 900,\n" +
            "  \"interval\": 5,\n" +
            "  \"message\": \"To sign in, use a web browser to open the page https://aka.ms/devicelogin and enter the code DW83JNP2P to authenticate.\"\n" +
            "}";

    @SuppressWarnings("unchecked")
    @Test
    public void deviceCodeFlowTest() throws Exception {
        app = PowerMock.createPartialMock(PublicClientApplication.class,
                new String[] { "acquireTokenCommon" },
                new PublicClientApplication.Builder(TestConfiguration.AAD_CLIENT_ID)
                        .authority(TestConfiguration.AAD_TENANT_ENDPOINT));

        Capture<MsalRequest> capturedMsalRequest = Capture.newInstance();

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.capture(capturedMsalRequest), EasyMock.isA(AADAuthority.class)).andReturn(
                AuthenticationResult.builder().
                        accessToken("accessToken").
                        expiresOn(new Date().getTime() + 100).
                        refreshToken("refreshToken").
                        idToken("idToken").environment("environment").build());

        PowerMock.mockStatic(HttpHelper.class);

        Capture<String> capturedUrl = Capture.newInstance();

        EasyMock.expect(
                HttpHelper.executeHttpRequest(
                        EasyMock.isA(Logger.class),
                        EasyMock.isA(HttpMethod.class),
                        EasyMock.capture(capturedUrl),
                        EasyMock.isA(Map.class),
                        EasyMock.isNull(),
                        EasyMock.isA(RequestContext.class),
                        EasyMock.isA(ServiceBundle.class)))
                .andReturn(INSTANCE_DISCOVERY_RESPONSE);

        EasyMock.expect(
                HttpHelper.executeHttpRequest(
                        EasyMock.isA(Logger.class),
                        EasyMock.isA(HttpMethod.class),
                        EasyMock.capture(capturedUrl),
                        EasyMock.isA(Map.class),
                        EasyMock.isNull(),
                        EasyMock.isA(RequestContext.class),
                        EasyMock.isA(ServiceBundle.class)))
                .andReturn(deviceCodeJsonResponse);

        PowerMock.replay(HttpHelper.class);

        AtomicReference<String> deviceCodeCorrelationId = new AtomicReference<>();

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) ->{

            // validate returned Device Code object
            Assert.assertNotNull(deviceCode);
            Assert.assertEquals(deviceCode.userCode(), "DW83JNP2P");
            Assert.assertEquals(deviceCode.deviceCode(), "DAQABAAEAAADRNYRQ3dhRFEeqWvq-yi6QodK2pb1iAA");
            Assert.assertEquals(deviceCode.verificationUri(), "https://aka.ms/devicelogin");
            Assert.assertEquals(deviceCode.expiresIn(), 900);
            Assert.assertEquals(deviceCode.interval(), 5);
            Assert.assertEquals(deviceCode.message(), "To sign in, use a web browser" +
                    " to open the page https://aka.ms/devicelogin and enter the code DW83JNP2P to authenticate.");
            Assert.assertNotNull(deviceCode.correlationId());

            deviceCodeCorrelationId.set(deviceCode.correlationId());
        };

        PowerMock.replay(app);

        IAuthenticationResult authResult = app.acquireToken
                (DeviceCodeFlowParameters.builder(Collections.singleton(AAD_RESOURCE_ID), deviceCodeConsumer)
                        .build())
                .get();

        // validate HTTP GET request used to get device code
        URL url = new URL(capturedUrl.getValue());
        Assert.assertEquals(url.getAuthority(), AAD_PREFERRED_NETWORK_ENV_ALIAS);
        Assert.assertEquals(url.getPath(),
                "/" + AAD_TENANT_NAME + AADAuthority.DEVICE_CODE_ENDPOINT);

        Map<String, String> expectedQueryParams = new HashMap<>();
        expectedQueryParams.put("client_id", AAD_CLIENT_ID);
        expectedQueryParams.put("scope", URLEncoder.encode(AbstractMsalAuthorizationGrant.COMMON_SCOPES_PARAM +
                AbstractMsalAuthorizationGrant.SCOPES_DELIMITER + AAD_RESOURCE_ID, "UTF-8" ));

        Assert.assertEquals(getQueryMap(url.getQuery()), expectedQueryParams);

        // make sure same correlation id is used for acquireDeviceCode and acquireTokenByDeviceCode calls

        Map<String, String > headers = capturedMsalRequest.getValue().headers().getReadonlyHeaderMap();
        Assert.assertEquals(capturedMsalRequest.getValue().headers().getReadonlyHeaderMap().
                get(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME), deviceCodeCorrelationId.get());
        Assert.assertNotNull(authResult);

        PowerMock.verify();
    }

    // TODO uncomment when ADFS support is added
//    @Test(expectedExceptions = IllegalArgumentException.class,
//            expectedExceptionsMessageRegExp = "Invalid authority type. Device Flow is only supported by AAD authority")
//    public void executeAcquireDeviceCode_AdfsAuthorityUsed_IllegalArgumentExceptionThrown()
//            throws Exception {
//
//        app = new PublicClientApplication.Builder("client_id")
//                .authority(ADFS_TENANT_ENDPOINT)
//                .validateAuthority(false).build();
//
//        app.acquireToken
//                (DeviceCodeFlowParameters
//                        .builder(Collections.singleton(AAD_RESOURCE_ID), (DeviceCode deviceCode) -> {})
//                        .build());
//    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Invalid authority type. Device Flow is only supported by AAD authority")
    public void executeAcquireDeviceCode_B2CAuthorityUsed_IllegalArgumentExceptionThrown()
            throws Exception {

        app = new PublicClientApplication.Builder("client_id")
                .b2cAuthority(TestConfiguration.B2C_AUTHORITY)
                .validateAuthority(false).build();

        app.acquireToken
                (DeviceCodeFlowParameters
                        .builder(Collections.singleton(AAD_RESOURCE_ID), (DeviceCode deviceCode) -> {})
                        .build());
    }


    @Test
    public void executeAcquireDeviceCode_AuthenticaionPendingErrorReturned_AuthenticationExceptionThrown()
            throws Exception {

        TelemetryManager telemetryManager =  new TelemetryManager(null, false);

        AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference =
                new AtomicReference<>();

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> { };

        app = new PublicClientApplication.Builder("client_id")
                .authority(AAD_TENANT_ENDPOINT)
                .validateAuthority(false)
                .build();

        DeviceCodeFlowParameters parameters =
                DeviceCodeFlowParameters.builder(Collections.singleton("default-scope"), deviceCodeConsumer)
                        .build();

        final DeviceCodeFlowRequest dcr =  new DeviceCodeFlowRequest(
                parameters,
                futureReference,
                app,
                new RequestContext(
                        "id",
                        "corr-id",
                        PublicApi.ACQUIRE_TOKEN_BY_DEVICE_CODE_FLOW));


        TokenRequest request = PowerMock.createPartialMock(
                TokenRequest.class, new String[]{"toOauthHttpRequest"},
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                dcr, new ServiceBundle(null, null, null, null, telemetryManager));

        OAuthHttpRequest msalOAuthHttpRequest = PowerMock.createMock(OAuthHttpRequest.class);

        HTTPResponse httpResponse = new HTTPResponse(HTTPResponse.SC_BAD_REQUEST);

        String content = "{\"error\":\"authorization_pending\"," +
                "\"error_description\":\"AADSTS70016: Pending end-user authorization.\\r\\n" +
                "Trace ID: 6c9dd244-0c65-4ea6-b121-0afd1c640200\\r\\n" +
                "Correlation ID: ff60101b-cb23-4a52-82cb-9966f466327a\\r\\n" +
                "Timestamp: 2018-03-14 20:15:43Z\"," +
                "\"error_codes\":[70016],\"timestamp\":\"2018-03-14 20:15:43Z\"," +
                "\"trace_id\":\"6c9dd244-0c65-4ea6-b121-0afd1c640200\"," +
                "\"correlation_id\":\"ff60101b-cb23-4a52-82cb-9966f466327a\"}";

        httpResponse.setContent(content);
        httpResponse.setContentType(CommonContentTypes.APPLICATION_JSON);

        EasyMock.expect(request.toOauthHttpRequest()).andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse).times(1);

        PowerMock.replay(request, msalOAuthHttpRequest);

        try {
            request.executeOauthRequestAndProcessResponse();
            Assert.fail("Expected MsalException was not thrown");
        } catch (MsalServiceException ex) {

            Assert.assertEquals(ex.errorCode(), AuthenticationErrorCode.AUTHORIZATION_PENDING);
        }
        PowerMock.verifyAll();
    }
}