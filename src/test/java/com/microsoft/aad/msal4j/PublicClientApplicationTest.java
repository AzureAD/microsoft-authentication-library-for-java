// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Future;

@PowerMockIgnore({"javax.net.ssl.*"})
@Test(groups = { "checkin" })
@PrepareForTest({ PublicClientApplication.class,
        AsymmetricKeyCredential.class, UserDiscoveryRequest.class})
public class PublicClientApplicationTest extends PowerMockTestCase {

    private PublicClientApplication app = null;

    @SuppressWarnings("unchecked")
    @Test
    public void testAcquireToken_Username_Password() throws Exception {
        app = PowerMock.createPartialMock(PublicClientApplication.class,
                new String[] { "acquireTokenCommon" },
                PublicClientApplication.builder(TestConfiguration.AAD_CLIENT_ID)
                        .authority(TestConfiguration.AAD_TENANT_ENDPOINT));

        PowerMock.expectPrivate(app, "acquireTokenCommon",
                EasyMock.isA(MsalRequest.class),
                EasyMock.isA(AADAuthority.class))
                .andReturn(AuthenticationResult.builder().
                        accessToken("accessToken").
                        expiresOn(new Date().getTime() + 100).
                        refreshToken("refreshToken").
                        idToken("idToken").environment("environment").build()
                );

        UserDiscoveryResponse response = EasyMock
                .createMock(UserDiscoveryResponse.class);
        EasyMock.expect(response.isAccountFederated()).andReturn(false);

        PowerMock.mockStatic(UserDiscoveryRequest.class);
        EasyMock.expect(
                UserDiscoveryRequest.execute(
                        EasyMock.isA(String.class),
                        EasyMock.isA(Map.class),
                        EasyMock.isA(RequestContext.class),
                        EasyMock.isA(ServiceBundle.class))).andReturn(response);

        PowerMock.replay(app, response, UserDiscoveryRequest.class);

        Future<IAuthenticationResult> result = app.acquireToken(
                UserNamePasswordParameters
                        .builder(Collections.singleton("scopes"), "username", "password".toCharArray())
                        .build());

        IAuthenticationResult ar = result.get();
        Assert.assertNotNull(ar);
        PowerMock.verifyAll();
        PowerMock.resetAll(app);
    }
}
