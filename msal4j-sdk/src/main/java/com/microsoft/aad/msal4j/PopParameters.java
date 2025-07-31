package com.microsoft.aad.msal4j;

import java.net.URI;

/**
 * Contains parameters used to request a Proof of Possession (PoP) token in supported flows
 */
public class PopParameters {

    HttpMethod httpMethod;
    URI uri;
    String nonce;

    /**
     * Gets the http method.
     * 
     * @return the http method
     */
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    /**
     * Gets the uri.
     * 
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Gets the nonce.
     * 
     * @return the nonce
     */
    public String getNonce() {
        return nonce;
    }

    PopParameters(HttpMethod httpMethod, URI uri, String nonce) {
        validatePopAuthScheme(httpMethod, uri);

        this.httpMethod = httpMethod;
        this.uri = uri;
        this.nonce = nonce;
    }

    /**
     * Performs any minimum validation to confirm this auth scheme could be valid for a POP request
     */
    void validatePopAuthScheme(HttpMethod httpMethod, URI uri) {
        //At a minimum HTTP method and host must be non-null
        if (httpMethod == null || uri == null || uri.getHost() == null) {
            throw new MsalClientException(
                    "HTTP method and URI host must be non-null", AuthenticationErrorCode.MSALJAVA_BROKERS_ERROR);
        }
    }
}
