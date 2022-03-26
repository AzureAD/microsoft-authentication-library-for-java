// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jose.JOSEObject;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.util.MultivaluedMapUtils;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Class to be used to acquire tokens for confidential client applications (Web Apps, Web APIs,
 * and daemon applications).
 * For details see {@link IConfidentialClientApplication}
 * <p>
 * Conditionally thread-safe
 */
public class ConfidentialClientApplication extends AbstractClientApplicationBase implements IConfidentialClientApplication {

    private ClientAuthentication clientAuthentication;
    private CustomJWTAuthentication customJWTAuthentication;
    private boolean clientCertAuthentication = false;
    private ClientCertificate clientCertificate;

    @Accessors(fluent = true)
    @Getter
    private boolean sendX5c;

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(ClientCredentialParameters parameters) {
        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_FOR_CLIENT,
                parameters);

        ClientCredentialRequest clientCredentialRequest =
                new ClientCredentialRequest(
                        parameters,
                        this,
                        context);

        return this.executeRequest(clientCredentialRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(OnBehalfOfParameters parameters) {
        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_ON_BEHALF_OF,
                parameters);

        OnBehalfOfRequest oboRequest = new OnBehalfOfRequest(
                parameters,
                this,
                context);

        return this.executeRequest(oboRequest);
    }

    private ConfidentialClientApplication(Builder builder) {
        super(builder);
        sendX5c = builder.sendX5c;

        log = LoggerFactory.getLogger(ConfidentialClientApplication.class);

        initClientAuthentication(builder.clientCredential);
    }

    private void initClientAuthentication(IClientCredential clientCredential) {
        validateNotNull("clientCredential", clientCredential);

        if (clientCredential instanceof ClientSecret) {
            clientAuthentication = new ClientSecretPost(
                    new ClientID(clientId()),
                    new Secret(((ClientSecret) clientCredential).clientSecret()));
        } else if (clientCredential instanceof ClientCertificate) {
            this.clientCertAuthentication = true;
            this.clientCertificate = (ClientCertificate) clientCredential;
            clientAuthentication = buildValidClientCertificateAuthority();
        } else if (clientCredential instanceof ClientAssertion) {
            clientAuthentication = createClientAuthFromClientAssertion((ClientAssertion) clientCredential);
        } else {
            throw new IllegalArgumentException("Unsupported client credential");
        }
    }

    @Override
    protected ClientAuthentication clientAuthentication() {
        if (clientCertAuthentication) {
            final Date currentDateTime = new Date(System.currentTimeMillis());
            final Date expirationTime = ((PrivateKeyJWT) clientAuthentication).getJWTAuthenticationClaimsSet().getExpirationTime();
            if (expirationTime.before(currentDateTime)) {
                //The asserted private jwt with the client certificate can expire so rebuild it when the
                clientAuthentication = buildValidClientCertificateAuthority();
            }
        }
        return clientAuthentication;
    }

    private ClientAuthentication buildValidClientCertificateAuthority() {
        ClientAssertion clientAssertion = JwtHelper.buildJwt(
                clientId(),
                clientCertificate,
                this.authenticationAuthority.selfSignedJwtAudience(),
                sendX5c);
        return createClientAuthFromClientAssertion(clientAssertion);
    }

    protected ClientAuthentication createClientAuthFromClientAssertion(
            final ClientAssertion clientAssertion) {
        final Map<String, List<String>> map = new HashMap<>();
        try {

            map.put("client_assertion_type", Collections.singletonList(ClientAssertion.assertionType));
            map.put("client_assertion", Collections.singletonList(clientAssertion.assertion()));
            return PrivateKeyJWT.parse(map);
        } catch (final ParseException e) {
            //This library is not supposed to validate Issuer and subject values.
            //The next lines of code ensures that exception is not thrown.
            if (e.getMessage().contains("Issuer and subject in client JWT assertion must designate the same client identifier")) {
                return new CustomJWTAuthentication(
                        ClientAuthenticationMethod.PRIVATE_KEY_JWT,
                        clientAssertion,
                        new ClientID(clientId())
                );
            }
            throw new MsalClientException(e);
        }
    }

    /**
     * Creates instance of Builder of ConfidentialClientApplication
     *
     * @param clientId         Client ID (Application ID) of the application as registered
     *                         in the application registration portal (portal.azure.com)
     * @param clientCredential The client credential to use for token acquisition.
     * @return instance of Builder of ConfidentialClientApplication
     */
    public static Builder builder(String clientId, IClientCredential clientCredential) {

        return new Builder(clientId, clientCredential);
    }

    public static class Builder extends AbstractClientApplicationBase.Builder<Builder> {

        private IClientCredential clientCredential;

        private boolean sendX5c = true;

        private Builder(String clientId, IClientCredential clientCredential) {
            super(clientId);
            this.clientCredential = clientCredential;
        }

        /**
         * Specifies if the x5c claim (public key of the certificate) should be sent to the STS.
         * Default value is true
         *
         * @param val true if the x5c should be sent. Otherwise false
         * @return instance of the Builder on which method was called
         */
        public ConfidentialClientApplication.Builder sendX5c(boolean val) {
            this.sendX5c = val;

            return self();
        }

        @Override
        public ConfidentialClientApplication build() {

            return new ConfidentialClientApplication(this);
        }

        @Override
        protected ConfidentialClientApplication.Builder self() {
            return this;
        }
    }
}