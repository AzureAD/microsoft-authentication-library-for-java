package com.microsoft.aad.msal4j;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

@PrepareForTest({HttpHelper.class, PublicClientApplication.class})
public class InstanceDiscoveryTest {

    private PublicClientApplication app;

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }

    @DataProvider(name = "aadClouds")
    private static Object[][] getAadClouds(){
        return new Object[][] {{"https://login.microsoftonline.com/common"} , // #Known to Microsoft
                {"https://private.cloud/foo"}//Private Cloud
        };
    }

    /**
     * when instance_discovery flag is set to true (by default), an instance_discovery is performed for authorityType = AAD
     */
    @Test(  dataProvider = "aadClouds")
    public void aadInstanceDiscoveryTrue(String authority) throws Exception{
        app = PowerMock.createPartialMock(PublicClientApplication.class,
                new String[]{"acquireTokenCommon"},
                PublicClientApplication.builder(TestConfiguration.AAD_CLIENT_ID)
                        .authority(authority));

        Capture<MsalRequest> capturedMsalRequest = Capture.newInstance();

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.capture(capturedMsalRequest), EasyMock.isA(AADAuthority.class)).andReturn(
                AuthenticationResult.builder().
                        accessToken("accessToken").
                        expiresOn(new Date().getTime() + 100).
                        refreshToken("refreshToken").
                        idToken("idToken").environment("environment").build());

        PowerMock.mockStatic(HttpHelper.class);

        HttpResponse instanceDiscoveryResponse = new HttpResponse();
        instanceDiscoveryResponse.statusCode(200);
        instanceDiscoveryResponse.body(TestConfiguration.INSTANCE_DISCOVERY_RESPONSE);

        Capture<HttpRequest> capturedHttpRequest = Capture.newInstance();

        EasyMock.expect(
                        HttpHelper.executeHttpRequest(
                                EasyMock.capture(capturedHttpRequest),
                                EasyMock.isA(RequestContext.class),
                                EasyMock.isA(ServiceBundle.class)))
                .andReturn(instanceDiscoveryResponse);

        PowerMock.replay(HttpHelper.class, HttpResponse.class);

        CompletableFuture<IAuthenticationResult> completableFuture = app.acquireToken(
                AuthorizationCodeParameters.builder
                                ("auth_code",
                                        new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI))
                        .scopes(Collections.singleton("default-scope"))
                        .build());

        completableFuture.get();
        Assert.assertEquals(capturedHttpRequest.getValues().size(),1);

    }

    /**
     * when instance_discovery flag is set to false, instance_discovery is not performed
     */
    @Test (dataProvider = "aadClouds")
    public void aadInstanceDiscoveryFalse(String authority) throws Exception {

        app = PowerMock.createPartialMock(PublicClientApplication.class,
                new String[]{"acquireTokenCommon"},
                PublicClientApplication.builder(TestConfiguration.AAD_CLIENT_ID)
                        .authority(authority)
                        .instanceDiscovery(false));

        Capture<MsalRequest> capturedMsalRequest = Capture.newInstance();

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.capture(capturedMsalRequest), EasyMock.isA(AADAuthority.class)).andReturn(
                AuthenticationResult.builder().
                        accessToken("accessToken").
                        expiresOn(new Date().getTime() + 100).
                        refreshToken("refreshToken").
                        idToken("idToken").environment("environment").build());

        PowerMock.mockStatic(HttpHelper.class);

        HttpResponse instanceDiscoveryResponse = new HttpResponse();
        instanceDiscoveryResponse.statusCode(200);
        instanceDiscoveryResponse.body(TestConfiguration.INSTANCE_DISCOVERY_RESPONSE);

        Capture<HttpRequest> capturedHttpRequest = Capture.newInstance();

        EasyMock.expect(
                        HttpHelper.executeHttpRequest(
                                EasyMock.capture(capturedHttpRequest),
                                EasyMock.isA(RequestContext.class),
                                EasyMock.isA(ServiceBundle.class)))
                .andReturn(instanceDiscoveryResponse);

        PowerMock.replay(HttpHelper.class, HttpResponse.class);

        CompletableFuture<IAuthenticationResult> completableFuture = app.acquireToken(
                AuthorizationCodeParameters.builder
                                ("auth_code",
                                        new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI))
                        .scopes(Collections.singleton("default-scope"))
                        .build());

        completableFuture.get();
        Assert.assertEquals(capturedHttpRequest.getValues().size(),0);
    }

    /**
     * when instance_discovery flag is set to true (by default), an instance_discovery is NOT performed for adfs.
     */
    @Test
    public void adfsInstanceDiscoveryTrue() throws Exception{
        app = PowerMock.createPartialMock(PublicClientApplication.class,
                new String[]{"acquireTokenCommon"},
                PublicClientApplication.builder(TestConstants.ADFS_APP_ID)
                        .authority("https://contoso.com/adfs")
                        .instanceDiscovery(true));

        Capture<MsalRequest> capturedMsalRequest = Capture.newInstance();

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.capture(capturedMsalRequest), EasyMock.isA(AADAuthority.class)).andReturn(
                AuthenticationResult.builder().
                        accessToken("accessToken").
                        expiresOn(new Date().getTime() + 100).
                        refreshToken("refreshToken").
                        idToken("idToken").environment("environment").build());

        PowerMock.mockStatic(HttpHelper.class);

        HttpResponse instanceDiscoveryResponse = new HttpResponse();
        instanceDiscoveryResponse.statusCode(200);
        instanceDiscoveryResponse.body(TestConfiguration.INSTANCE_DISCOVERY_RESPONSE);

        Capture<HttpRequest> capturedHttpRequest = Capture.newInstance();

        EasyMock.expect(
                        HttpHelper.executeHttpRequest(
                                EasyMock.capture(capturedHttpRequest),
                                EasyMock.isA(RequestContext.class),
                                EasyMock.isA(ServiceBundle.class)))
                .andReturn(instanceDiscoveryResponse);

        PowerMock.replay(HttpHelper.class, HttpResponse.class);

        CompletableFuture<IAuthenticationResult> completableFuture = app.acquireToken(
                AuthorizationCodeParameters.builder
                                ("auth_code",
                                        new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI))
                        .scopes(Collections.singleton("default-scope"))
                        .build());

        completableFuture.get();
        Assert.assertEquals(capturedHttpRequest.getValues().size(),0);

    }

    /**
     * when instance_discovery flag is set to true (by default), an instance_discovery is NOT performed for b2c.
     */
    @Test
    public void b2cInstanceDiscoveryTrue() throws Exception{
        app = PowerMock.createPartialMock(PublicClientApplication.class,
                new String[]{"acquireTokenCommon"},
                PublicClientApplication.builder(TestConstants.ADFS_APP_ID)
                        .b2cAuthority(TestConstants.B2C_MICROSOFTLOGIN_ROPC)
                        .instanceDiscovery(true));

        Capture<MsalRequest> capturedMsalRequest = Capture.newInstance();

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.capture(capturedMsalRequest), EasyMock.isA(AADAuthority.class)).andReturn(
                AuthenticationResult.builder().
                        accessToken("accessToken").
                        expiresOn(new Date().getTime() + 100).
                        refreshToken("refreshToken").
                        idToken("idToken").environment("environment").build());

        PowerMock.mockStatic(HttpHelper.class);

        HttpResponse instanceDiscoveryResponse = new HttpResponse();
        instanceDiscoveryResponse.statusCode(200);
        instanceDiscoveryResponse.body(TestConfiguration.INSTANCE_DISCOVERY_RESPONSE);

        Capture<HttpRequest> capturedHttpRequest = Capture.newInstance();

        EasyMock.expect(
                        HttpHelper.executeHttpRequest(
                                EasyMock.capture(capturedHttpRequest),
                                EasyMock.isA(RequestContext.class),
                                EasyMock.isA(ServiceBundle.class)))
                .andReturn(instanceDiscoveryResponse);

        PowerMock.replay(HttpHelper.class, HttpResponse.class);

        CompletableFuture<IAuthenticationResult> completableFuture = app.acquireToken(
                AuthorizationCodeParameters.builder
                                ("auth_code",
                                        new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI))
                        .scopes(Collections.singleton("default-scope"))
                        .build());

        completableFuture.get();
        Assert.assertEquals(capturedHttpRequest.getValues().size(),0);

    }


}
