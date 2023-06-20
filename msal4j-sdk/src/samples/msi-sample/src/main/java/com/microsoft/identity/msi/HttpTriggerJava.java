package com.microsoft.identity.msi;

import java.util.*;

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.ManagedIdentityApplication;
import com.microsoft.aad.msal4j.ManagedIdentityId;
import com.microsoft.aad.msal4j.ManagedIdentityParameters;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Function with HTTP Trigger to test managed identity.
 */
public class HttpTriggerJava {
    /**
     * This function listens at endpoint "/api/MsiFunctionJava". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/MsiFunctionJava
     * 2. curl {your host}/api/MsiFunctionJava?resource=HTTP%20Query
     */
    @FunctionName("MsiFunctionJava")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        String query = request.getQueryParameters().get("resource");
        String resource = request.getBody().orElse(query);

        if (resource == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a resource to acquire token for").build();
        }

        String response;

        try {
            ManagedIdentityApplication msiApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.SystemAssigned())
                    .logPii(true)
                    .build();
            IAuthenticationResult result = msiApp.acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(resource).build()).get();

            response = "Access token: " + result.accessToken().substring(10) + "\nScopes: " + result.scopes();
        } catch (Exception exception) {
            response = exception.getMessage();
        }

        return request.createResponseBuilder(HttpStatus.OK).body(response).build();

    }
}
