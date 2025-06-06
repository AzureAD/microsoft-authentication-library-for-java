package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.msal4j.Shortcuts.TestConfig;
import com.microsoft.aad.msal4j.Shortcuts.TestObject;
import com.microsoft.aad.msal4j.Shortcuts.TestStep;
import com.microsoft.aad.msal4j.Shortcuts.TestAction;

//TODO: Too specific for the test case used in this proof-of-concept, should be able to reuse the regular JsonHelper class
class RunnerJsonHelper {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Parse test configuration from JSON string
     */
    static TestConfig parseTestConfig(String jsonContent) {
        try {
            return JsonParser.parseConfig(mapper.readTree(jsonContent));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse test configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Helper class for parsing JSON into test configuration
     */
    private static class JsonParser {
        static TestConfig parseConfig(JsonNode rootNode) {
            TestConfig.Builder builder = new TestConfig.Builder()
                    .type(rootNode.path("type").asText())
                    .version(rootNode.path("ver").asInt());

            parseEnvironment(rootNode.path("env"), builder);
            parseArrangement(rootNode.path("arrange"), builder);
            parseSteps(rootNode.path("steps"), builder);

            return builder.build();
        }

        private static void parseEnvironment(JsonNode envNode, TestConfig.Builder builder) {
            envNode.fields().forEachRemaining(entry ->
                    builder.addEnvironmentVariable(entry.getKey(), entry.getValue().asText()));
        }

        private static void parseArrangement(JsonNode arrangeNode, TestConfig.Builder builder) {
            arrangeNode.fields().forEachRemaining(appEntry -> {
                String appName = appEntry.getKey();
                JsonNode appNode = appEntry.getValue();

                appNode.fields().forEachRemaining(classEntry -> {
                    String classType = classEntry.getKey();
                    JsonNode classNode = classEntry.getValue();

                    Map<String, JsonNode> properties = new HashMap<>();
                    classNode.fields().forEachRemaining(prop ->
                            properties.put(prop.getKey(), prop.getValue()));

                    builder.addArrangedObject(appName,
                            new TestObject(appName, classType, properties));
                });
            });
        }

        private static void parseSteps(JsonNode stepsNode, TestConfig.Builder builder) {
            for (JsonNode stepNode : stepsNode) {
                TestAction action = parseAction(stepNode.path("act"));
                Map<String, JsonNode> assertions = parseAssertions(stepNode.path("assert"));

                if (action != null) {
                    builder.addStep(new TestStep(action, assertions));
                }
            }
        }

        private static TestAction parseAction(JsonNode actNode) {
            if (actNode.isMissingNode()) return null;

            String actorKey = actNode.fieldNames().next();
            String[] actorParts = actorKey.split("\\.", 2);

            Map<String, JsonNode> parameters = new HashMap<>();
            actNode.get(actorKey).fields().forEachRemaining(entry ->
                    parameters.put(entry.getKey(), entry.getValue()));

            return new TestAction(actorParts[0], actorParts[1], parameters);
        }

        private static Map<String, JsonNode> parseAssertions(JsonNode assertNode) {
            Map<String, JsonNode> assertions = new HashMap<>();

            if (!assertNode.isMissingNode()) {
                assertNode.fields().forEachRemaining(entry ->
                        assertions.put(entry.getKey(), entry.getValue()));
            }

            return assertions;
        }
    }
}
