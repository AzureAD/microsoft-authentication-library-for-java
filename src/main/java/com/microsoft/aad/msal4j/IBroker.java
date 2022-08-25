package com.microsoft.aad.msal4j;

/**
 * Used to define the basic set of methods that all Brokers must implement
 */
public interface IBroker {
    void initializeBroker ();

    Account signInSilently(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters);

    Account signInInteractively(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters);

    AuthenticationResult acquireTokenSilently(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters, Account account);

    AuthenticationResult acquireTokenInteractively(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters, Account account);

    void signOutSilently(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters);

    //TODO: too specific to MSALRuntime? Should it be split into a generic getAccount(something) and getAllAccounts(clientId)?
    void discoverAccounts(PublicClientApplication clientApplication, InteractiveRequestParameters interactiveRequestParameters);
}
