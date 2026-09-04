// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class DefaultHttpClientMtlsTest {

    @Test
    void requestSpecificMtlsDisablesRedirects() throws Exception {
        DefaultHttpClient client =
                new DefaultHttpClient(null, null, null, null);

        HttpsURLConnection connection = (HttpsURLConnection)
                client.openConnection(
                        new URL("https://localhost/token"),
                        mock(SSLSocketFactory.class));

        assertFalse(connection.getInstanceFollowRedirects());
    }
}
