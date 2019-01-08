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
