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
public class WSTrustResponseTest {

    @BeforeTest
    public void setup(){
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory","com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }
    
    @AfterTest
    public void cleanup(){
        System.clearProperty("javax.xml.parsers.DocumentBuilderFactory");
    }
    
    @Test
    public void testWSTrustResponseParseSuccess() throws Exception {
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
        Assert.assertNotNull(response);
    }

    @Test(expectedExceptions = Exception.class, expectedExceptionsMessageRegExp = "Server returned error in RSTR - ErrorCode: RequestFailed : FaultMessage: MSIS3127: The specified request failed.")
    public void testWSTrustResponseParseError() throws Exception {

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(
                (this.getClass().getResource(
                        TestConfiguration.AAD_TOKEN_ERROR_FILE).getFile())))) {
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
        }
        WSTrustResponse response = WSTrustResponse.parse(sb.toString(), WSTrustVersion.WSTRUST13);
        Assert.assertNotNull(response);
    }
}
