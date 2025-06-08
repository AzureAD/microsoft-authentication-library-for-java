package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

class Shortcuts {
    //Various helpers and utilities for simplifying the first proof-of-concept implementation.
    //They don't follow any design doc or language conventions, and would not be part of the final implementation.

    //=====The following static classes would ideally extend Azure JSON's JsonSerializable or some equivalent YAML parsing interface,
    // but for now they are simple classes representing the structure of a test configuration.
    static class TestConfig {
        private final String type;
        private final int version;
        private final Map<String, String> environment;
        private final Map<String, Shortcuts.TestObject> arrangeObjects;
        private final List<Shortcuts.TestStep> steps;
        private static final ObjectMapper mapper = new ObjectMapper();

        private TestConfig(Shortcuts.TestConfig.Builder builder) {
            this.type = builder.type;
            this.version = builder.version;
            this.environment = Collections.unmodifiableMap(builder.environment);
            this.arrangeObjects = Collections.unmodifiableMap(builder.arrangeObjects);
            this.steps = Collections.unmodifiableList(builder.steps);
        }

        String getType() {
            return type;
        }

        int getVersion() {
            return version;
        }

        String getEnvironmentVariable(String name) {
            return environment.get(name);
        }

        Map<String, String> getAllEnvironmentVariables() {
            return environment;
        }

        Shortcuts.TestObject getArrangeObject(String name) {
            return arrangeObjects.get(name);
        }

        List<String> getAllArrangeObjects() {
            return new ArrayList<>(arrangeObjects.keySet());
        }

        List<Shortcuts.TestStep> getSteps() {
            return steps;
        }

        static class Builder {
            private String type;
            private int version;
            private final Map<String, String> environment = new HashMap<>();
            private final Map<String, Shortcuts.TestObject> arrangeObjects = new HashMap<>();
            private final List<Shortcuts.TestStep> steps = new ArrayList<>();

            Shortcuts.TestConfig.Builder type(String type) {
                this.type = type;
                return this;
            }

            Shortcuts.TestConfig.Builder version(int version) {
                this.version = version;
                return this;
            }

            Shortcuts.TestConfig.Builder addEnvironmentVariable(String name, String value) {
                environment.put(name, value);
                return this;
            }

            Shortcuts.TestConfig.Builder addArrangedObject(String name, Shortcuts.TestObject object) {
                arrangeObjects.put(name, object);
                return this;
            }

            Shortcuts.TestConfig.Builder addStep(Shortcuts.TestStep step) {
                steps.add(step);
                return this;
            }

            Shortcuts.TestConfig build() {
                return new Shortcuts.TestConfig(this);
            }
        }
    }

    static class TestObject {
        private final String name;
        private final String type;
        private final Map<String, JsonNode> properties;

        TestObject(String name, String type, Map<String, JsonNode> properties) {
            this.name = name;
            this.type = type;
            this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
        }

        String getName() {
            return name;
        }

        String getType() {
            return type;
        }

        JsonNode getProperty(String name) {
            return properties.get(name);
        }
    }

    static class TestStep {
        private final Shortcuts.TestAction action;
        private final Map<String, JsonNode> assertions;

        TestStep(Shortcuts.TestAction action, Map<String, JsonNode> assertions) {
            this.action = action;
            this.assertions = Collections.unmodifiableMap(new HashMap<>(assertions));
        }

        Shortcuts.TestAction getAction() {
            return action;
        }

        JsonNode getAssertion(String key) {
            return assertions.get(key);
        }

        Map<String, JsonNode> getAssertions() {
            return assertions;
        }
    }

    static class TestAction {
        private final String targetObject;
        private final String methodName;
        private final Map<String, JsonNode> parameters;

        TestAction(String targetObject, String methodName, Map<String, JsonNode> parameters) {
            this.targetObject = targetObject;
            this.methodName = methodName;
            this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
        }

        String getTargetObject() {
            return targetObject;
        }

        String getMethodName() {
            return methodName;
        }

        JsonNode getParameter(String name) {
            return parameters.get(name);
        }

        boolean hasParameter(String name) {
            return parameters.containsKey(name);
        }

        Map<String, JsonNode> getParameters() {
            return parameters;
        }
    }

    //=====The following methods are small fixes for issues in the test configuration JSONs

    //Some URLs in the test configurations are malformed and are missing a slash before "test".
    static String fixAzureWebsiteUrls(String jsonString) {
        return jsonString.replace(
                "https://smile-test.azurewebsites.nettest/token",
                "https://smile-test.azurewebsites.net/test/token");
    }

    //Some test configurations use a claims challenge that is not valid JSON.
    static String validateAndGetClaimsChallenge(TestAction action) {
        if (!action.hasParameter("claims_challenge")) {
            return null;
        }

        String claimsChallenge = action.getParameter("claims_challenge").asText();
        try {
            // Try to parse the claims challenge as JSON
            new ObjectMapper().readTree(claimsChallenge);
            return claimsChallenge;
        } catch (Exception e) {
            return TestConfiguration.CLAIMS_CHALLENGE;
        }
    }

    //The server has a public list of endpoints leading to .sml files, but there are JSON equivalents for those files at endpoints ending with .json
    static List<String> convertSmlUrlsToJsonUrls(List<String> smlUrls) {
        return smlUrls.stream()
                .filter(url -> url.endsWith(".sml"))
                .map(url -> url.substring(0, url.length() - 4) + ".json")
                .collect(Collectors.toList());
    }
}