package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.util.URLUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Getter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
class InteractiveRequest extends MsalRequest{

    AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference;
    private PublicClientApplication publicClientApplication;

    URI authorizationURI;
    InteractiveRequestParameters interactiveRequestParameters;

    private String verifier;
    private String state;

    InteractiveRequest(InteractiveRequestParameters parameters,
                       AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference,
                       PublicClientApplication publicClientApplication,
                       RequestContext requestContext){

        super(publicClientApplication, null, requestContext);

        this.interactiveRequestParameters = parameters;
        this.futureReference = futureReference;
        this.publicClientApplication = publicClientApplication;
        this.authorizationURI = createAuthorizationURI();
        validateRedirectURI(parameters.redirectUri());

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

            if (!redirectURI.getScheme().equals("http")) {
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

        addPkceAndState(requestParameters);

        String scopesParam = AbstractMsalAuthorizationGrant.COMMON_SCOPES_PARAM +
                AbstractMsalAuthorizationGrant.SCOPES_DELIMITER +
                String.join(" ", interactiveRequestParameters.scopes());

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

    private void addPkceAndState(Map<String, List<String>> requestParameters) {

        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        requestParameters.put("code_challenge", Collections.singletonList(
                StringHelper.createBase64EncodedSha256Hash(verifier)));
        requestParameters.put("code_challenge_method", Collections.singletonList("S256"));

        state = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        requestParameters.put("state", Collections.singletonList(state));
    }
}
