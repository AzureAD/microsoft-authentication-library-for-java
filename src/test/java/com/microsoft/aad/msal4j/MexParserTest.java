// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.BufferedReader;
import java.io.FileReader;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

@Test(groups = { "checkin" })
public class MexParserTest {

    @BeforeTest
    public void setup(){
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }
    
    @AfterTest
    public void cleanup(){
        System.clearProperty("javax.xml.parsers.DocumentBuilderFactory");
    }
    
    @Test
    public void testMexParsing() throws Exception {

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
        Assert.assertEquals(endpoint.getUrl(),
                "https://msft.sts.microsoft.com/adfs/services/trust/13/usernamemixed");
    }
    
    @Test
    public void testMexParsingWs2005() throws Exception {

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
        Assert.assertEquals(endpoint.getUrl(),"https://msft.sts.microsoft.com/adfs/services/trust/2005/usernamemixed");
    }

    @Test
    public void testMexParsingIntegrated() throws Exception {

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
        Assert.assertEquals(endpoint.getUrl(),
                "https://msft.sts.microsoft.com/adfs/services/trust/13/windowstransport");
    }
}
