package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.util.URLUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InteractiveRequest extends MsalRequest{

    URI authorizationURI;
    InteractiveRequestParameters interactiveRequestParameters;
    private PublicClientApplication publicClientApplication;

    InteractiveRequest(InteractiveRequestParameters parameters,
                       PublicClientApplication publicClientApplication,
                       RequestContext requestContext){

        super(publicClientApplication, null, requestContext);

        validateRedirectURI(parameters.redirectUri());
        this.authorizationURI = createAuthorizationURI();
        this.interactiveRequestParameters = parameters;
        this.publicClientApplication = publicClientApplication;
    }

    private void validateRedirectURI(URI redirectURI) {
        try {
            if (!InetAddress.getByName(redirectURI.getHost()).isLoopbackAddress()) {
                throw new MsalClientException(String.format(
                        "Only loopback redirect uri is supported, but %s was found " +
                                "Configure http://localhost or http://localhost:port both during app registration" +
                                "and when you create the create the InteractiveRequestParameters object", redirectURI.getHost()),
                        AuthenticationErrorCode.LOOPBACK_REDIRECT_URI);
            }

            if (redirectURI.getScheme() != null) {
                throw new MsalClientException(String.format(
                        "Only http uri scheme is supported but %s was found. Configure http://localhost" +
                                "or http://localhost:port both during app registration and when you create" +
                                " the create the InteractiveRequestParameters object", redirectURI.toString()),
                        AuthenticationErrorCode.LOOPBACK_REDIRECT_URI);
            }
        } catch (UnknownHostException exception){
            throw new MsalClientException(exception);
        }
    }

    private URI createAuthorizationURI() {

        URI uri;
        try{
            Map<String, List<String>> authorizationRequestParameters = createAuthorizationRequestParameters();
            String authorizationCodeEndpoint = publicClientApplication.authority() + "oauth2/v2.0/authorize";
            String uriString = authorizationCodeEndpoint + "?" + URLUtils.serializeParameters(authorizationRequestParameters);

            uri = new URI(uriString);
        } catch (URISyntaxException exception) {
            throw new MsalClientException(exception);
        }

    return uri;
    }

    private Map<String, List<String>> createAuthorizationRequestParameters(){

        Map<String, List<String>> requestParameters = new HashMap<>();

        String scopesParam = AbstractMsalAuthorizationGrant.COMMON_SCOPES_PARAM +
                AbstractMsalAuthorizationGrant.SCOPES_DELIMITER + interactiveRequestParameters.scopes();

        requestParameters.put("scope", Collections.singletonList(scopesParam));
        requestParameters.put("response_type", Collections.singletonList("code"));
        requestParameters.put("response_mode", Collections.singletonList("query"));
        requestParameters.put("client_id", Collections.singletonList(
                publicClientApplication.clientId()));
        requestParameters.put("redirect_uri", Collections.singletonList(
                interactiveRequestParameters.redirectUri().toString()));
        requestParameters.put("correlation_id", Collections.singletonList(
                publicClientApplication.correlationId()));

        return requestParameters;
    }
}
