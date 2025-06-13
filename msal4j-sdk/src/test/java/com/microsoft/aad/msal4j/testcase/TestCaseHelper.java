// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.testcase;

import com.azure.json.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TestCaseHelper implements JsonSerializable<TestCaseHelper> {
    private static final Logger LOG = LoggerFactory.getLogger(TestCaseHelper.class);
    private final List<String> testcases;

    /**
     * Creates a new TestCaseHelper with the given list of test case URLs.
     * @param testcases List of test case URLs
     */
    public TestCaseHelper(List<String> testcases) {
        this.testcases = testcases;
    }

    /**
     * Returns the list of test case URLs.
     * @return List of test case URLs
     */
    public List<String> getTestcases() {
        return testcases;
    }

    /**
     * Creates a TestCaseHelper from a JSON reader.
     * @param jsonReader The JSON reader
     * @return A new TestCaseHelper instance
     * @throws IOException If there's an error reading the JSON
     */
    public static TestCaseHelper fromJson(JsonReader jsonReader) throws IOException {
        List<String> urls = new ArrayList<>();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();

                if ("testcases".equals(fieldName)) {
                    reader.nextToken(); // Move to START_ARRAY

                    // Read array elements until END_ARRAY
                    while (reader.nextToken() != JsonToken.END_ARRAY) {
                        urls.add(reader.getString());
                    }
                } else {
                    reader.nextToken(); // Move to the value
                    reader.skipChildren();
                }
            }

            return new TestCaseHelper(urls);
        });
    }

    /**
     * Fetches test case URLs from an endpoint containing JSON with a "testcases" array.
     *
     * @param endpointUrl The URL to fetch test cases from
     * @return List of test case URLs
     * @throws IOException If there's an error fetching or parsing the JSON
     */
    public static List<String> getTestCaseUrlsFromEndpoint(String endpointUrl) throws IOException {
        String jsonContent = fetchRawContent(endpointUrl);
        JsonReader reader = JsonProviders.createReader(
                new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8)));

        TestCaseHelper helper = fromJson(reader);
        return helper.getTestcases();
    }

    /**
     * Fetches raw content from a URL as a string
     *
     * @param url The URL to fetch content from
     * @return The content as a string
     * @throws IOException If there's an error fetching the content
     */
    private static String fetchRawContent(String url) throws IOException {
        URL requestUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                StringBuilder response = new StringBuilder();
                char[] buffer = new char[1024];
                int charsRead;
                while ((charsRead = reader.read(buffer)) != -1) {
                    response.append(buffer, 0, charsRead);
                }
                return response.toString();
            }
        } else {
            throw new IOException("HTTP request failed with status code: " + responseCode);
        }
    }

    /**
     * Fetches JSON content from a URL
     *
     * @param jsonUrl The URL to fetch JSON content from
     * @return The JSON content as a string
     * @throws IOException If there's an error fetching the content
     */
    public static String fetchJsonContent(String jsonUrl) throws IOException {
        return fetchRawContent(jsonUrl);
    }

    /**
     * Complete workflow to get all test case configs
     *
     * @param indexEndpoint The URL of the index containing test case URLs
     * @return Map of test case names to their TestCase instances
     * @throws IOException If there's an error fetching or parsing the JSON
     */
    public static Map<String, TestCase> getAllTestCaseConfigs(String indexEndpoint) throws IOException {
        // Get list of SML test case URLs
        List<String> smlUrls = getTestCaseUrlsFromEndpoint(indexEndpoint);

        // Convert SML URLs to JSON URLs
        List<String> jsonUrls = convertSmlUrlsToJsonUrls(smlUrls);

        // Fetch content for each JSON URL
        Map<String, TestCase> testCaseConfigs = new HashMap<>();
        for (String jsonUrl : jsonUrls) {
            String testCaseName = extractTestCaseName(jsonUrl);
            String jsonContent = fetchJsonContent(jsonUrl);

            JsonReader reader = JsonProviders.createReader(
                    new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8)));
            TestCase testCase = TestCase.fromJson(reader);
            testCaseConfigs.put(testCaseName, testCase);
        }

        return testCaseConfigs;
    }

    /**
     * Convert SML URLs to JSON URLs
     */
    private static List<String> convertSmlUrlsToJsonUrls(List<String> smlUrls) {
        List<String> jsonUrls = new ArrayList<>();
        for (String smlUrl : smlUrls) {
            jsonUrls.add(smlUrl.replace(".sml", ".json"));
        }
        return jsonUrls;
    }

    /**
     * Extract test case name from URL
     */
    private static String extractTestCaseName(String url) {
        String[] parts = url.split("/");
        String fileName = parts[parts.length - 1];
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeStartArray("testcases");

        for (String testcase : testcases) {
            jsonWriter.writeString(testcase);
        }

        jsonWriter.writeEndArray();
        jsonWriter.writeEndObject();

        return jsonWriter;
    }
}