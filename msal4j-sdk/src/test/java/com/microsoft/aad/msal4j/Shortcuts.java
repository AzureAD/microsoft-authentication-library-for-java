package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

class Shortcuts {
    //Various helpers and utilities for simplifying the first proof-of-concept implementation.
    //They don't follow any design doc, and would not be part of the final implementation.

    //Represents test cases that are stored in the server. JSON is used here because the library already knows how to parse JSON,
    // but the 'real' implementation could use another format.
    private static final String MI_CAPABILITY_SML = "{\n" +
            "  \"type\": \"MSAL Test\",\n" +
            "  \"ver\": 1,\n" +
            "  \"env\": {\n" +
            "    \"IDENTITY_ENDPOINT\": \"fill in\",\n" +
            "    \"IDENTITY_HEADER\": \"foo\",\n" +
            "    \"IDENTITY_SERVER_THUMBPRINT\": \"bar\"\n" +
            "  },\n" +
            "  \"arrange\": {\n" +
            "    \"app1\": {\n" +
            "      \"ManagedIdentityClient\": {\n" +
            "        \"managed_identity\": {\n" +
            "          \"ManagedIdentityIdType\": \"SystemAssigned\",\n" +
            "          \"Id\": null\n" +
            "        },\n" +
            "        \"client_capabilities\": [\"cp1\", \"cp2\"]\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"steps\": [\n" +
            "    {\n" +
            "      \"act\": {\n" +
            "        \"app1.AcquireTokenForManagedIdentity\": {\n" +
            "          \"resource\": \"R\"\n" +
            "        }\n" +
            "      },\n" +
            "      \"assert\": {\n" +
            "        \"token_type\": \"Bearer\",\n" +
            "        \"token_source\": \"identity_provider\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"act\": {\n" +
            "        \"app1.AcquireTokenForManagedIdentity\": {\n" +
            "          \"resource\": \"R\"\n" +
            "        }\n" +
            "      },\n" +
            "      \"assert\": {\n" +
            "        \"token_type\": \"Bearer\",\n" +
            "        \"token_source\": \"cache\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"act\": {\n" +
            "        \"app1.AcquireTokenForManagedIdentity\": {\n" +
            "          \"resource\": \"R\",\n" +
            "          \"claims_challenge\": \"{\\\"capability test case likely needs\\\": \\\"a valid json object\\\"}\"\n" +
            "        }\n" +
            "      },\n" +
            "      \"assert\": {\n" +
            "        \"token_type\": \"Bearer\",\n" +
            "        \"token_source\": \"identity_provider\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    //Represents a response from the tests cases server, would be
    static String getTestConfigJson() {
            return MI_CAPABILITY_SML;
        }

        //The following static classes would ideally extend Azure JSON's JsonSerializable or some equivalent YAML parsing interface,
        // but for now they are simple classes representing the structure of a test configuration.
        static class TestConfig {
            private final String type;
            private final int version;
            private final Map<String, String> environment;
            private final Map<String, Shortcuts.TestObject> arrangeObjects;
            private final List<Shortcuts.TestStep> steps;

            private TestConfig(Shortcuts.TestConfig.Builder builder) {
                this.type = builder.type;
                this.version = builder.version;
                this.environment = Collections.unmodifiableMap(builder.environment);
                this.arrangeObjects = Collections.unmodifiableMap(builder.arrangeObjects);
                this.steps = Collections.unmodifiableList(builder.steps);
            }

            String getType() { return type; }
            int getVersion() { return version; }
            String getEnvironmentVariable(String name) { return environment.get(name); }
            Map<String, String> getAllEnvironmentVariables() { return environment; }
            Shortcuts.TestObject getArrangeObject(String name) { return arrangeObjects.get(name); }
            List<String> getAllArrangeObjects() { return new ArrayList<>(arrangeObjects.keySet()); }
            List<Shortcuts.TestStep> getSteps() { return steps; }

            static class Builder {
                private String type;
                private int version;
                private final Map<String, String> environment = new HashMap<>();
                private final Map<String, Shortcuts.TestObject> arrangeObjects = new HashMap<>();
                private final List<Shortcuts.TestStep> steps = new ArrayList<>();

                Shortcuts.TestConfig.Builder type(String type) { this.type = type; return this; }
                Shortcuts.TestConfig.Builder version(int version) { this.version = version; return this; }
                Shortcuts.TestConfig.Builder addEnvironmentVariable(String name, String value) { environment.put(name, value); return this; }
                Shortcuts.TestConfig.Builder addArrangedObject(String name, Shortcuts.TestObject object) { arrangeObjects.put(name, object); return this; }
                Shortcuts.TestConfig.Builder addStep(Shortcuts.TestStep step) { steps.add(step); return this; }

                Shortcuts.TestConfig build() { return new Shortcuts.TestConfig(this); }
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

            String getName() { return name; }
            String getType() { return type; }
            JsonNode getProperty(String name) { return properties.get(name); }
        }

        static class TestStep {
            private final Shortcuts.TestAction action;
            private final Map<String, JsonNode> assertions;

            TestStep(Shortcuts.TestAction action, Map<String, JsonNode> assertions) {
                this.action = action;
                this.assertions = Collections.unmodifiableMap(new HashMap<>(assertions));
            }

            Shortcuts.TestAction getAction() { return action; }
            JsonNode getAssertion(String key) { return assertions.get(key); }
            Map<String, JsonNode> getAssertions() { return assertions; }
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

        String getTargetObject() { return targetObject; }
        String getMethodName() { return methodName; }
        JsonNode getParameter(String name) { return parameters.get(name); }
        boolean hasParameter(String name) { return parameters.containsKey(name); }
        Map<String, JsonNode> getParameters() { return parameters; }
    }
}
