package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Getter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
class InteractiveRequest extends MsalRequest{

    AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference;
    private PublicClientApplication publicClientApplication;

    URL authorizationUrl;
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
        this.authorizationUrl = createAuthorizationUrl();
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

    private URL createAuthorizationUrl(){

        Set<String> scopesParam = new HashSet<>(interactiveRequestParameters.scopes());
        String[] commonScopes = AbstractMsalAuthorizationGrant.COMMON_SCOPES_PARAM.split(" ");
        scopesParam.addAll(Arrays.asList(commonScopes));

        AuthorizationRequestUrl.Builder authorizationRequestUrlBuilder =
                AuthorizationRequestUrl
                        .builder(publicClientApplication.clientId(),
                                interactiveRequestParameters.redirectUri().toString(),
                                scopesParam)
                .correlationId(publicClientApplication.correlationId());

        AuthorityType authorityType = publicClientApplication.authenticationAuthority.authorityType;
        if(authorityType == AuthorityType.AAD || authorityType == AuthorityType.ADFS) {
            authorizationRequestUrlBuilder
                    .authority(publicClientApplication.authority());
        } else if(authorityType == AuthorityType.B2C){
            authorizationRequestUrlBuilder
                    .b2cAuthority(publicClientApplication.authority());
        }
        addPkceAndState(authorizationRequestUrlBuilder);

        return authorizationRequestUrlBuilder.build().authorizationRequestUrl();
    }


//    private URI createAuthorizationURI() {
//
//        URI uri;
//        try{
//            Map<String, List<String>> authorizationRequestParameters = createAuthorizationRequestParameters();
//            String authorizationCodeEndpoint = publicClientApplication.authority() + "oauth2/v2.0/authorize";
//            String uriString = authorizationCodeEndpoint + "?" +
//                    URLUtils.serializeParameters(authorizationRequestParameters);
//
//            uri = new URI(uriString);
//        } catch (URISyntaxException exception) {
//            throw new MsalClientException(exception);
//        }
//
//    return uri;
//    }
//
//    private Map<String, List<String>> createAuthorizationRequestParameters(){
//
//        Map<String, List<String>> requestParameters = new HashMap<>();
//
//        addPkceAndState(requestParameters);
//
//        String scopesParam = AbstractMsalAuthorizationGrant.COMMON_SCOPES_PARAM +
//                AbstractMsalAuthorizationGrant.SCOPES_DELIMITER +
//                String.join(" ", interactiveRequestParameters.scopes());
//
//        requestParameters.put("scope", Collections.singletonList(scopesParam));
//        requestParameters.put("response_type", Collections.singletonList("code"));
//        requestParameters.put("response_mode", Collections.singletonList("form_post"));
//        requestParameters.put("clientId", Collections.singletonList(
//                publicClientApplication.clientId()));
//        requestParameters.put("redirect_uri", Collections.singletonList(
//                interactiveRequestParameters.redirectUri().toString()));
//        requestParameters.put("correlation_id", Collections.singletonList(
//                publicClientApplication.correlationId()));
//
//        return requestParameters;
//    }

    private void addPkceAndState(AuthorizationRequestUrl.Builder builder) {

        // Create code verifier and code challenge as described in https://tools.ietf.org/html/rfc7636
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        state = UUID.randomUUID().toString() + UUID.randomUUID().toString();

        builder.codeChallenge(StringHelper.createBase64EncodedSha256Hash(verifier))
                .codeChallengeMethod("256")
                .state(state);
    }
}
