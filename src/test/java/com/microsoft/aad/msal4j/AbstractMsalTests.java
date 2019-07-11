// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.powermock.modules.testng.PowerMockTestCase;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AbstractMsalTests extends PowerMockTestCase {

    public void beforeClass() throws IOException {

        Properties prop = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream(this.getClass()
                    .getResource(TestConfiguration.AAD_CERTIFICATE_PATH)
                    .getFile());
            prop.getProperty("database");
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }
}
