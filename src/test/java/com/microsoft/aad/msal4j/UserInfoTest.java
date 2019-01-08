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

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import com.nimbusds.jwt.JWTClaimsSet;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
@Test(groups = { "checkin" })
@PrepareForTest(JWTClaimsSet.class)
public class UserInfoTest extends AbstractAdalTests {

    @Test
    public void testCreateFromIdTokenClaims_EmptyClaims() throws ParseException {

        final JWTClaimsSet claimSet = PowerMock
                .createMock(JWTClaimsSet.class);
        EasyMock.expect(claimSet.getClaims())
                .andReturn(new HashMap<String, Object>()).times(1);
        EasyMock.replay(claimSet);
        Assert.assertNull(UserInfo.createFromIdTokenClaims(claimSet));
        PowerMock.verifyAll();
    }

    @Test
    public void testCreateFromIdTokenClaims_Null() throws ParseException {

        Assert.assertNull(UserInfo.createFromIdTokenClaims(null));
    }

    @Test
    public void testCreateFromIdTokenClaims_HasEmailSubjectPasswordClaims()
            throws ParseException {

        final JWTClaimsSet claimSet = PowerMock
                .createMock(JWTClaimsSet.class);
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("", "");
        EasyMock.expect(claimSet.getClaims()).andReturn(map).times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_OBJECT_ID))
                .andReturn(null).times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_SUBJECT))
                .andReturn("sub").times(2);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_UPN))
                .andReturn(null).times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_EMAIL))
                .andReturn("test@value.com").times(2);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_GIVEN_NAME))
                .andReturn("test").times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_FAMILY_NAME))
                .andReturn("value").times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_IDENTITY_PROVIDER))
                .andReturn("idp").times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_PASSWORD_CHANGE_URL))
                .andReturn("url").times(2);
        EasyMock.expect(
                claimSet.getClaim(AuthenticationConstants.ID_TOKEN_PASSWORD_EXPIRES_ON))
                .andReturn("5000").times(2);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_TENANTID))
                .andReturn("TenantID").times(1);

        EasyMock.replay(claimSet);
        final UserInfo ui = UserInfo.createFromIdTokenClaims(claimSet);
        Assert.assertNotNull(ui);
        Assert.assertEquals("test@value.com", ui.getDisplayableId());
        Assert.assertEquals("sub", ui.getUniqueId());
        Assert.assertEquals("test", ui.getGivenName());
        Assert.assertEquals("value", ui.getFamilyName());
        Assert.assertEquals("idp", ui.getIdentityProvider());
        Assert.assertEquals("url", ui.getPasswordChangeUrl());
        Assert.assertEquals("TenantID", ui.getTenantId());

        Assert.assertNotNull(ui.getPasswordExpiresOn());
        PowerMock.verifyAll();
    }

    public void testCreateFromIdTokenClaims_HasUpnObjectIdNoPasswordClaims()
            throws ParseException {

        final JWTClaimsSet claimSet = PowerMock
                .createMock(JWTClaimsSet.class);
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("", "");
        EasyMock.expect(claimSet.getClaims()).andReturn(map).times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_OBJECT_ID))
                .andReturn(null).times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_SUBJECT))
                .andReturn("sub").times(2);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_UPN))
                .andReturn(null).times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_EMAIL))
                .andReturn("test@value.com").times(2);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_GIVEN_NAME))
                .andReturn("test").times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_FAMILY_NAME))
                .andReturn("value").times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_IDENTITY_PROVIDER))
                .andReturn("idp").times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_PASSWORD_CHANGE_URL))
                .andReturn(null).times(1);
        EasyMock.expect(
                claimSet.getClaim(AuthenticationConstants.ID_TOKEN_PASSWORD_EXPIRES_ON))
                .andReturn(null).times(1);
        EasyMock.expect(
                claimSet.getStringClaim(AuthenticationConstants.ID_TOKEN_TENANTID))
                .andReturn("TenantID").times(1);

        EasyMock.replay(claimSet);
        final UserInfo ui = UserInfo.createFromIdTokenClaims(claimSet);
        Assert.assertNotNull(ui);
        Assert.assertEquals("test@value.com", ui.getDisplayableId());
        Assert.assertEquals("sub", ui.getUniqueId());
        Assert.assertEquals("test", ui.getGivenName());
        Assert.assertEquals("value", ui.getFamilyName());
        Assert.assertEquals("idp", ui.getIdentityProvider());
        Assert.assertNull(ui.getPasswordChangeUrl());
        Assert.assertNull(ui.getPasswordExpiresOn());
        Assert.assertEquals("TenantID", ui.getTenantId());
        PowerMock.verifyAll();
    }
}
