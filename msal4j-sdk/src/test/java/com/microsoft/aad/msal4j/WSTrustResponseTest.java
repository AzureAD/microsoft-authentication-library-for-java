// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.BufferedReader;
import java.io.FileReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WSTrustResponseTest {

    @BeforeAll
    void setup() {
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }

    @AfterAll
    void cleanup() {
        System.clearProperty("javax.xml.parsers.DocumentBuilderFactory");
    }

    @Test
    void testWSTrustResponseParseSuccess() throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader((this
                .getClass().getResource(
                        TestConfiguration.AAD_TOKEN_SUCCESS_FILE).getFile())))) {
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
        }
        WSTrustResponse response = WSTrustResponse.parse(sb.toString(), WSTrustVersion.WSTRUST13);
        assertNotNull(response);
    }
}
