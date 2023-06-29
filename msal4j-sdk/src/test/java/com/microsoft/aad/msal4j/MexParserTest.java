// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.BufferedReader;
import java.io.FileReader;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MexParserTest {

    @BeforeAll
    void setup() {
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }

    @AfterAll
    void cleanup() {
        System.clearProperty("javax.xml.parsers.DocumentBuilderFactory");
    }

    @Test
    void testMexParsing() throws Exception {

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(
                (this.getClass().getResource(
                        TestConfiguration.AAD_MEX_RESPONSE_FILE).getFile())))) {
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
        }

        BindingPolicy endpoint = MexParser.getWsTrustEndpointFromMexResponse(sb.toString(), false);
        assertEquals(endpoint.getUrl(),
                "https://msft.sts.microsoft.com/adfs/services/trust/13/usernamemixed");
    }

    @Test
    void testMexParsingWs2005() throws Exception {

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(
                this.getClass().getResource(
                        TestConfiguration.AAD_MEX_2005_RESPONSE_FILE).getFile()))) {
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
        }
        BindingPolicy endpoint = MexParser.getWsTrustEndpointFromMexResponse(sb
                .toString(), false);
        assertEquals(endpoint.getUrl(), "https://msft.sts.microsoft.com/adfs/services/trust/2005/usernamemixed");
    }

    @Test
    void testMexParsingIntegrated() throws Exception {

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(
                (this.getClass().getResource(
                        TestConfiguration.AAD_MEX_RESPONSE_FILE_INTEGRATED).getFile())))) {
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
        }
        BindingPolicy endpoint = MexParser.getPolicyFromMexResponseForIntegrated(sb
                .toString(), false);
        assertEquals(endpoint.getUrl(),
                "https://msft.sts.microsoft.com/adfs/services/trust/13/windowstransport");
    }
}
