// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TestHelper {

    //Signed JWT which should be enough to pass the parsing/validation in the library, useful if a unit test needs an
    // assertion but that is not the focus of the test
    static String signedAssertion = generateToken();
    private static final String successfulResponseFormat = "{\"access_token\":\"%s\",\"id_token\":\"%s\",\"refresh_token\":\"%s\"," +
            "\"client_id\":\"%s\",\"client_info\":\"%s\"," +
            "\"expires_on\": %d ,\"expires_in\": %d," +
            "\"token_type\":\"Bearer\"}";

    static final String idTokenFormat = "{\"aud\": \"%s\"," +
            "\"iss\": \"%s\"," +
            "\"iat\": 1455833828," + "\"nbf\": 1455833828," + "\"exp\": 1455837728," +
            "\"ipaddr\": \"131.107.159.117\"," +
            "\"name\": \"%s\"," +
            "\"oid\": \"%s\"," +
            "\"preferred_username\": \"%s\"," +
            "\"sub\": \"%s\"," +
            "\"tid\": \"%s\"," +
            "\"ver\": \"2.0\"}";

    static String readResource(Class<?> classInstance, String resource) {
        try {
            return new String(Files.readAllBytes(Paths.get(classInstance.getResource(resource).toURI())));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    static void deleteFileContent(Class<?> classInstance, String resource)
            throws URISyntaxException, IOException {
        FileWriter fileWriter = new FileWriter(
                new File(Paths.get(classInstance.getResource(resource).toURI()).toString()),
                false);

        fileWriter.write("");
        fileWriter.close();
    }

    static String generateToken() {
        try {
            RSAKey rsaJWK = new RSAKeyGenerator(2048)
                    .keyID("kid")
                    .generate();
            JWSObject jwsObject = new JWSObject(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(),
                    new Payload("payload"));

            jwsObject.sign(new RSASSASigner(rsaJWK));

            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    //Maps various values to the successfulResponseFormat string to create a valid token response
    static String getSuccessfulTokenResponse(HashMap<String, String> responseValues) {
        //Will default to expiring in one hour if expiry time values are not set
        long expiresIn = responseValues.containsKey("expires_in") ?
                Long.parseLong(responseValues.get("expires_in")) :
                3600;
        long expiresOn = responseValues.containsKey("expires_on")
                ? Long.parseLong(responseValues.get("expires_0n")) :
                (System.currentTimeMillis() / 1000) + expiresIn;

        return String.format(successfulResponseFormat,
                responseValues.getOrDefault("access_token", "access_token"),
                responseValues.getOrDefault("id_token", ""),
                responseValues.getOrDefault("refresh_token", "refresh_token"),
                responseValues.getOrDefault("client_id", "client_id"),
                responseValues.getOrDefault("client_info", "eyJ1aWQiOiI1OTdmODZjZC0xM2YzLTQ0YzAtYmVjZS1hMWU3N2JhNDMyMjgiLCJ1dGlkIjoiZjY0NWFkOTItZTM4ZC00ZDFhLWI1MTAtZDFiMDlhNzRhOGNhIn0"),
                expiresOn,
                expiresIn
        );
    }

    //Creates a valid HttpResponse that can be used when mocking HttpClient.send()
    static HttpResponse expectedResponse(int statusCode, String response) {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList("application/json"));

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.statusCode(statusCode);
        httpResponse.body(response);
        httpResponse.addHeaders(headers);

        return httpResponse;
    }

    //Maps various values to the idTokenFormat string
    static String createIdToken(HashMap<String, String> idTokenValues) {
        String tokenValues = String.format(idTokenFormat,
                idTokenValues.getOrDefault("aud", "e854a4a7-6c34-449c-b237-fc7a28093d84"),
                idTokenValues.getOrDefault("iss", "https://login.microsoftonline.com/6c3d51dd-f0e5-4959-b4ea-a80c4e36fe5e/v2.0/"),
                idTokenValues.getOrDefault("name", "name"),
                idTokenValues.getOrDefault("oid", "oid"),
                idTokenValues.getOrDefault("preferred_username", "preferred_username"),
                idTokenValues.getOrDefault("sub", "K4_SGGxKqW1SxUAmhg6C1F6VPiFzcx-Qd80ehIEdFus"),
                idTokenValues.getOrDefault("client_info", "eyJ1aWQiOiI1OTdmODZjZC0xM2YzLTQ0YzAtYmVjZS1hMWU3N2JhNDMyMjgiLCJ1dGlkIjoiZjY0NWFkOTItZTM4ZC00ZDFhLWI1MTAtZDFiMDlhNzRhOGNhIn0")
        );

        String encodedTokenValues = Base64.getUrlEncoder().encodeToString(tokenValues.getBytes());

        return String.format("someheader.%s.somesignature", encodedTokenValues);
    }
}