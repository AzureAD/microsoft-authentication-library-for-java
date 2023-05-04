package com.microsoft.aad.msal4j;

import java.net.URI;

/**
 * Contains parameters used to request a Proof of Possession (PoP) token in supported flows
 */
class PopParameters {

    String httpMethod;
    URI uri;
    String nonce;

    public String getHttpMethod() {
        return httpMethod;
    }

    public URI getUri() {
        return uri;
    }

    public String getNonce() {
        return nonce;
    }

    PopParameters(String httpMethod, URI uri, String nonce) {
        validatePopAuthScheme(httpMethod, uri);

        this.httpMethod = httpMethod;
        this.uri = uri;
        this.nonce = nonce;
    }

    /**
     * Performs any minimum validation to confirm this auth scheme could be valid for a POP request
     */
    void validatePopAuthScheme(String httpMethod, URI uri) {
        //At a minimum HTTP method and host must be non-null
        if (httpMethod == null || uri == null || uri.getHost() == null) {
            throw new MsalClientException(
                    "HTTP method and URI host must be non-null", "TBD");//TODO: error code
        }
    }
}
