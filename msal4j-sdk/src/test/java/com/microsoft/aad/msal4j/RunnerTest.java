package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import com.microsoft.aad.msal4j.Shortcuts.TestConfig;
import com.microsoft.aad.msal4j.Shortcuts.TestStep;

import java.util.Map;

class RunnerTest {

    @Test
    void testManagedIdentityWithJsonConfig() throws Exception {
        //TODO: get test cases list from the server
        TestConfig config = RunnerJsonHelper.parseTestConfig(Shortcuts.getTestConfigJson());
        Map<String, ManagedIdentityApplication> apps = RunnerHelper.createAppsFromConfig(config);

        for (ManagedIdentityApplication app : apps.values()) {
            //Execute the "steps" section of the test config
            for (TestStep step : config.getSteps()) {
                //Execute the "act" section of the test config
                IAuthenticationResult result = RunnerHelper.executeAction(app, step.getAction());

                //Execute the "assert" section of the test config
                RunnerHelper.validateAssertions(result, step.getAssertions());
            }
        }
    }
}
