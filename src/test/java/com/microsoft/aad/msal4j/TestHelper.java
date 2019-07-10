// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

class TestHelper {

    static String readResource(Class<?> classInstance, String resource) throws IOException, URISyntaxException {
        return new String(
                Files.readAllBytes(
                        Paths.get(classInstance.getResource(resource).toURI())));
    }

    static void deleteFileContent(Class<?> classInstance, String resource)
            throws URISyntaxException, IOException {
        FileWriter fileWriter = new FileWriter(
                new File(Paths.get(classInstance.getResource(resource).toURI()).toString()),
                false);

        fileWriter.write("");
        fileWriter.close();
    }
}
