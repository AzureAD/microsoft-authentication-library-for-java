package com.microsoft.msi;

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.ManagedIdentityApplication;
import com.microsoft.aad.msal4j.ManagedIdentityId;
import com.microsoft.aad.msal4j.ManagedIdentityParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Sample app to test MSI using a single jar.
 * To create a jar run mvn install -Dmaven.test.skip=true for msal4j pom and then msi-sample-jar pom.
 * Copy the jar with dependencies and run on a VM or cloud shell using java -jar msi-sample-jar-1.0.1-jar-with-dependencies.jar
 */
public class App 
{
    public static void main( String[] args ) throws IOException {
        String response;
        String resource = "https://management.azure.com";
        int option = 1;
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
        final Logger logger = LoggerFactory.getLogger(App.class);

        while (option != 0) {
            System.out.println("Enter one of the following options to create a managed identity application:");
            System.out.println("1: System assigned managed identity");
            System.out.println("2: User assigned managed identity");
            System.out.println("0: Quit");

            option = Integer.parseInt(reader.readLine());

            switch (option) {
                case 1:
                    acquireTokenWithSAMI(resource, logger, reader);
                    break;
                case 2:
                    acquireTokenWithUAMI(resource, logger, reader);
                    break;
                case 0:
                    return;
                default:
                    System.out.println("Invalid option, try again.");
            }

        }
    }

    private static void acquireTokenWithSAMI(String resource, Logger logger, BufferedReader reader) throws IOException {
        System.out.println("Enter a scope to acquire token for.");
        resource = reader.readLine();

        ManagedIdentityApplication msiApp = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .logPii(true)
                .build();

        try {
            logger.info("Trying to acquire a token for system assigned managed identity with provided resource.");
            IAuthenticationResult result = msiApp.acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(resource).build()).get();
            logger.info("Access token recieved: " + result.accessToken().substring(0, 10) + "\nScopes: " + result.scopes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void acquireTokenWithUAMI(String resource, Logger logger, BufferedReader reader) throws IOException {
        System.out.println("Enter the options to create a user assigned managed identity");
        System.out.println("1. User assigned client id.");
        System.out.println("2. User assigned resource id.");

        int option = Integer.parseInt(reader.readLine());

        ManagedIdentityId msiId;

        switch (option) {
            case 1:
                System.out.println("Enter client id of the user assigned managed identity");
                String clientId = reader.readLine();
                msiId = ManagedIdentityId.userAssignedClientId(clientId);
                break;
            case 2:
                System.out.println("Enter resource id of the user assigned managed identity");
                String resourceId = reader.readLine();
                msiId = ManagedIdentityId.userAssignedResourceId(resourceId);
                break;
            default:
                System.out.println("Invalid option");
                return;
        }

        System.out.println("Enter a scope to acquire token for. ");
        resource = reader.readLine();

        ManagedIdentityApplication msiApp = ManagedIdentityApplication
                .builder(msiId)
                .logPii(true)
                .build();

        try {
            logger.info("Trying to acquire a token for system assigned managed identity with provided resource.");
            IAuthenticationResult result = msiApp.acquireTokenForManagedIdentity(ManagedIdentityParameters.builder(resource).build()).get();
            logger.info("Access token received: " + result.accessToken().substring(0, 10) + "\nScopes: " + result.scopes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
