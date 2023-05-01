package com.microsoft.aad.msal4j;

import java.net.URI;

//TODO: javadocs explaining this class, link to documentation page
public class AuthScheme {

    String httpMethod;
    URI uri;
    String nonce;
    SchemeType type;

    public enum SchemeType {
        POP
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public URI getUri() {
        return uri;
    }

    public String getNonce() {
        return nonce;
    }

    public SchemeType getType() {
        return type;
    }

    private AuthScheme(String httpMethod, URI uri, String nonce, SchemeType type) {
        this.httpMethod = httpMethod;
        this.uri = uri;
        this.nonce = nonce;
        this.type = type;
    }

    /**
     * Returns an AuthScheme used to request a POP token in supported flows
     *
     * @param httpMethod a valid HTTP method, such as "GET" or "POST"
     * @param uri URI to associate with the token
     * @param nonce optional nonce value for the token, can be empty or null
     */
    public static AuthScheme popAuthScheme(String httpMethod, URI uri, String nonce) {
        //HTTP method must be uppercase when sent to MSALRuntime
        AuthScheme popScheme = new AuthScheme(httpMethod.toUpperCase(), uri, nonce, SchemeType.POP);
        popScheme.validatePopAuthScheme();

        return popScheme;
    }

    /**
     * Performs any minimum validation to confirm this auth scheme could be valid for a POP request
     */
    void validatePopAuthScheme() {
        //At a minimum HTTP method and host must be non-null
        if (httpMethod == null || uri == null || uri.getHost() == null) {
            throw new MsalClientException(
                    "HTTP method and URI host must be non-null", "TBD");//TODO: error code
        }
    }
}
