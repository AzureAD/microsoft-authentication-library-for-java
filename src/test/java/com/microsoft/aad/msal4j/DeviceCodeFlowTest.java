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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.TestConfiguration.AAD_CLIENT_ID;
import static com.microsoft.aad.msal4j.TestConfiguration.AAD_HOST_NAME;
import static com.microsoft.aad.msal4j.TestConfiguration.AAD_RESOURCE_ID;
import static com.microsoft.aad.msal4j.TestConfiguration.AAD_TENANT_NAME;
import static com.microsoft.aad.msal4j.TestConfiguration.ADFS_TENANT_ENDPOINT;


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
            "  \"verification_url\": \"https://aka.ms/devicelogin\",\n" +
            "  \"expires_in\": \"900\",\n" +
            "  \"interval\": \"5\",\n" +
            "  \"message\": \"To sign in, use a web browser to open the page https://aka.ms/devicelogin and enter the code DW83JNP2P to authenticate.\"\n" +
            "}";

    @Test
    public void deviceCodeFlowTest() throws Exception {
        app = PowerMock.createPartialMock(PublicClientApplication.class,
                new String[] { "acquireTokenCommon" },
                new PublicClientApplication.Builder(TestConfiguration.AAD_CLIENT_ID)
                        .authority(TestConfiguration.AAD_TENANT_ENDPOINT));

        Capture<MsalRequest> capturedMsalRequest = Capture.newInstance();

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.capture(capturedMsalRequest)).andReturn(
                new AuthenticationResult("bearer", "accessToken",
                        "refreshToken", new Date().getTime(), "idToken", null,
                        false));

        PowerMock.mockStatic(HttpHelper.class);

        Capture<String> capturedUrl = Capture.newInstance();

        EasyMock.expect(
                HttpHelper.executeHttpGet(EasyMock.isA(Logger.class), EasyMock.capture(capturedUrl),
                        EasyMock.isA(Map.class), EasyMock.isA(ServiceBundle.class)))
                .andReturn(deviceCodeJsonResponse);

        PowerMock.replay(HttpHelper.class);

        AtomicReference<String> deviceCodeCorrelationId = new AtomicReference<>();

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) ->{

            // validate returned Device Code object
            Assert.assertNotNull(deviceCode);
            Assert.assertEquals(deviceCode.getUserCode(), "DW83JNP2P");
            Assert.assertEquals(deviceCode.getDeviceCode(), "DAQABAAEAAADRNYRQ3dhRFEeqWvq-yi6QodK2pb1iAA");
            Assert.assertEquals(deviceCode.getVerificationUrl(), "https://aka.ms/devicelogin");
            Assert.assertEquals(deviceCode.getExpiresIn(), 900);
            Assert.assertEquals(deviceCode.getInterval(), 5);
            Assert.assertEquals(deviceCode.getMessage(), "To sign in, use a web browser" +
                    " to open the page https://aka.ms/devicelogin and enter the code DW83JNP2P to authenticate.");
            Assert.assertNotNull(deviceCode.getCorrelationId());

            deviceCodeCorrelationId.set(deviceCode.getCorrelationId());
        };

        PowerMock.replay(app);

        AuthenticationResult authResult =
                app.acquireTokenByDeviceCodeFlow(Collections.singleton(AAD_RESOURCE_ID), deviceCodeConsumer).get();

        // validate HTTP GET request used to get device code
        URL url = new URL(capturedUrl.getValue());
        Assert.assertEquals(url.getAuthority(), AAD_HOST_NAME);
        Assert.assertEquals(url.getPath(),
                "/" + AAD_TENANT_NAME + AuthenticationAuthority.DEVICE_CODE_ENDPOINT);

        Map<String, String> expectedQueryParams = new HashMap<>();
        expectedQueryParams.put("client_id", AAD_CLIENT_ID);
        expectedQueryParams.put("scope", URLEncoder.encode(MsalAuthorizationGrant.COMMON_SCOPES_PARAM +
                MsalAuthorizationGrant.SCOPES_DELIMITER + AAD_RESOURCE_ID));

        Assert.assertEquals(getQueryMap(url.getQuery()), expectedQueryParams);

        // make sure same correlation id is used for acquireDeviceCode and acquireTokenByDeviceCode calls

        Map<String, String > headers = capturedMsalRequest.getValue().getHeaders().getReadonlyHeaderMap();
        Assert.assertEquals(capturedMsalRequest.getValue().getHeaders().getReadonlyHeaderMap().
                get(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME), deviceCodeCorrelationId.get());
        Assert.assertNotNull(authResult);

        PowerMock.verify();
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Unsupported authority type")
    public void executeAcquireDeviceCode_AdfsAuthorityUsed_IllegalArgumentExceptionThrown()
            throws Exception {

        app = new PublicClientApplication.Builder("client_id")
                .authority(ADFS_TENANT_ENDPOINT)
                .validateAuthority(false).build();

        app.acquireTokenByDeviceCodeFlow(Collections.singleton(AAD_RESOURCE_ID), (DeviceCode deviceCode) -> {});
    }

    @Test
    public void executeAcquireDeviceCode_AuthenticaionPendingErrorReturned_AuthenticationExceptionThrown()
            throws Exception {

        TokenEndpointRequest request = PowerMock.createPartialMock(
                TokenEndpointRequest.class, new String[]{"toOauthHttpRequest"},
                new URL("http://login.windows.net"), null, null);

        OauthHttpRequest msalOauthHttpRequest = PowerMock
                .createMock(OauthHttpRequest.class);

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

        EasyMock.expect(request.toOauthHttpRequest()).andReturn(msalOauthHttpRequest).times(1);
        EasyMock.expect(msalOauthHttpRequest.send()).andReturn(httpResponse).times(1);

        PowerMock.replay(request, msalOauthHttpRequest);

        try {
            request.executeOauthRequestAndProcessResponse();
            Assert.fail("Expected AuthenticationException was not thrown");
        } catch (AuthenticationException ex) {
            Assert.assertEquals(ex.getErrorCode(), AuthenticationErrorCode.AUTHORIZATION_PENDING);
        }
        PowerMock.verifyAll();
    }
}
