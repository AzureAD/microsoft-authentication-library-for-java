package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.AdalErrorCode.AUTHORIZATION_PENDING;

public class AcquireTokenDeviceCodeFlowSupplier extends MsalSupplier {

    private ClientAuthentication clientAuth;
    private String scopes;
    private Consumer<DeviceCode> deviceCodeConsumer;
    private AtomicReference<CompletableFuture<AuthenticationResult>> futureReference;

    AcquireTokenDeviceCodeFlowSupplier(PublicClientApplication clientApplication, ClientAuthentication clientAuth,
                                       Set<String> scopes, Consumer<DeviceCode> deviceCodeConsumer,
                                       AtomicReference<CompletableFuture<AuthenticationResult>> futureReference)
    {
        super(clientApplication);
        this.headers = new ClientDataHttpHeaders(clientApplication.getCorrelationId());
        this.clientAuth = clientAuth;
        this.scopes = String.join(" ", scopes);
        this.deviceCodeConsumer = deviceCodeConsumer;

        this.futureReference = futureReference;
    }

    AuthenticationResult execute() throws Exception {

        clientApplication.authenticationAuthority.doInstanceDiscovery(clientApplication.isValidateAuthority(),
                headers.getReadonlyHeaderMap(), clientApplication.getProxy(), clientApplication.getSslSocketFactory());

        DeviceCode deviceCode = DeviceCodeRequest.execute(clientApplication.authenticationAuthority.getDeviceCodeEndpoint(),
                clientAuth.getClientID().toString(), scopes, headers.getReadonlyHeaderMap(), clientApplication.getProxy(),
                clientApplication.getSslSocketFactory());

        deviceCodeConsumer.accept(deviceCode);

        MsalDeviceCodeAuthorizationGrant deviceCodeGrant =
                new MsalDeviceCodeAuthorizationGrant(deviceCode, deviceCode.getScopes());

        long expirationTimeInSeconds =
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + deviceCode.getExpiresIn();

        AcquireTokenSupplier acquireTokenSupplier =
                new AcquireTokenSupplier(clientApplication, deviceCodeGrant, clientAuth);

        while (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) < expirationTimeInSeconds) {
            if(futureReference.get().isCancelled()){
                throw new InterruptedException("Acquire token Device Code Flow was interrupted");
            }
            try {
                return acquireTokenSupplier.execute();
            }
            catch (AuthenticationException ex) {
                if (ex.getErrorCode().equals(AUTHORIZATION_PENDING))
                {
                    TimeUnit.SECONDS.sleep(deviceCode.getInterval());
                } else {
                    throw ex;
                }
            }
        }
        throw new AuthenticationException("Expired Device code");
    }
}
