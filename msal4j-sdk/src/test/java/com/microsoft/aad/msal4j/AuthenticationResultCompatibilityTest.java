// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AuthenticationResultCompatibilityTest {
    private static final AuthenticationResultMetadata SHARED_METADATA =
            AuthenticationResultMetadata.builder().build();
    private static final String LEGACY_BEARER_FIXTURE =
            "rO0ABXNyAC1jb20ubWljcm9zb2Z0LmFhZC5tc2FsNGouQXV0aGVudGljYXRpb25SZXN1bHQAAAAAAAAAAQIAEEoACWV4cGly"
            + "ZXNPbkoADGV4dEV4cGlyZXNPbkwAC2FjY2Vzc1Rva2VudAASTGphdmEvbGFuZy9TdHJpbmc7TAAHYWNjb3VudHQAI0xjb20v"
            + "bWljcm9zb2Z0L2FhZC9tc2FsNGovSUFjY291bnQ7TAASYWNjb3VudENhY2hlRW50aXR5dAAtTGNvbS9taWNyb3NvZnQvYWFk"
            + "L21zYWw0ai9BY2NvdW50Q2FjaGVFbnRpdHk7TAALZW52aXJvbm1lbnRxAH4AAUwADWV4cGlyZXNPbkRhdGV0ABBMamF2YS91"
            + "dGlsL0RhdGU7TAAIZmFtaWx5SWRxAH4AAUwAB2lkVG9rZW5xAH4AAUwADWlkVG9rZW5PYmplY3R0ACJMY29tL21pY3Jvc29m"
            + "dC9hYWQvbXNhbDRqL0lkVG9rZW47TAASaXNQb3BBdXRob3JpemF0aW9udAATTGphdmEvbGFuZy9Cb29sZWFuO0wACG1ldGFk"
            + "YXRhdAA3TGNvbS9taWNyb3NvZnQvYWFkL21zYWw0ai9BdXRoZW50aWNhdGlvblJlc3VsdE1ldGFkYXRhO0wACXJlZnJlc2hP"
            + "bnQAEExqYXZhL2xhbmcvTG9uZztMAAxyZWZyZXNoVG9rZW5xAH4AAUwABnNjb3Blc3EAfgABTAANdGVuYW50UHJvZmlsZXQA"
            + "KUxjb20vbWljcm9zb2Z0L2FhZC9tc2FsNGovSVRlbmFudFByb2ZpbGU7eHAAAAAAZVPxAAAAAABlU/IsdAATbGVnYWN5LWFj"
            + "Y2Vzcy10b2tlbnBwdAAZbG9naW4ubWljcm9zb2Z0b25saW5lLmNvbXNyAA5qYXZhLnV0aWwuRGF0ZWhqgQFLWXQZAwAAeHB3"
            + "CAAAAYvP5WgAeHBwcHBzcgA1Y29tLm1pY3Jvc29mdC5hYWQubXNhbDRqLkF1dGhlbnRpY2F0aW9uUmVzdWx0TWV0YWRhdGFb"
            + "mvGlR+Uw2QIAA0wAEmNhY2hlUmVmcmVzaFJlYXNvbnQALUxjb20vbWljcm9zb2Z0L2FhZC9tc2FsNGovQ2FjaGVSZWZyZXNo"
            + "UmVhc29uO0wACXJlZnJlc2hPbnEAfgAITAALdG9rZW5Tb3VyY2V0ACZMY29tL21pY3Jvc29mdC9hYWQvbXNhbDRqL1Rva2Vu"
            + "U291cmNlO3hwfnIAK2NvbS5taWNyb3NvZnQuYWFkLm1zYWw0ai5DYWNoZVJlZnJlc2hSZWFzb24AAAAAAAAAABIAAHhyAA5q"
            + "YXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AA5OT1RfQVBQTElDQUJMRXBwcHB0AAVzY29wZXA=";

    @Test
    void deserializesPreTokenTypeBearerFixtureAsBearer() throws Exception {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(
                Base64.getDecoder().decode(LEGACY_BEARER_FIXTURE)))) {
            AuthenticationResult result = (AuthenticationResult) input.readObject();
            assertEquals("legacy-access-token", result.accessToken());
            assertEquals(TokenType.BEARER.value(), result.tokenType());
        }
    }

    @Test
    void equalityUsesNormalizedSchemeAndStableBindingIdentityOnly() {
        AuthenticationResult legacyNull = result(null, null, null);
        AuthenticationResult bearer = result(TokenType.BEARER.value(), null, null);
        BindingCertificate bindingA = new BindingCertificate(
                Collections.singletonList(TestHelper.getX509Cert()), "same-binding");
        BindingCertificate bindingB = new BindingCertificate(
                Collections.singletonList(TestHelper.getX509Cert()), "same-binding");
        IMtlsBindingContext contextA = org.mockito.Mockito.mock(IMtlsBindingContext.class);
        IMtlsBindingContext contextB = org.mockito.Mockito.mock(IMtlsBindingContext.class);
        AuthenticationResult popA = result(TokenType.MTLS_POP.value(), bindingA, contextA);
        AuthenticationResult popB = result(TokenType.MTLS_POP.value(), bindingB, contextB);

        assertEquals(legacyNull, bearer);
        assertEquals(legacyNull.hashCode(), bearer.hashCode());
        assertNotEquals(bearer, popA);
        assertEquals(popA, popB);
        assertEquals(popA.hashCode(), popB.hashCode());
    }

    private static AuthenticationResult result(
            String tokenType,
            BindingCertificate bindingCertificate,
            IMtlsBindingContext context) {
        return AuthenticationResult.builder()
                .accessToken("token")
                .expiresOn(100)
                .extExpiresOn(200)
                .environment("environment")
                .scopes("scope")
                .metadata(SHARED_METADATA)
                .tokenType(tokenType)
                .bindingCertificate(bindingCertificate)
                .mtlsBindingContext(context)
                .build();
    }
}
