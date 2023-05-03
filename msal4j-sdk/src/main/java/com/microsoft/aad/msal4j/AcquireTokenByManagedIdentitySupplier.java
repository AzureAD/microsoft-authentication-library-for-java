package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class AcquireTokenByManagedIdentitySupplier extends AuthenticationResultSupplier{

    private final static Logger LOG = LoggerFactory.getLogger(AcquireTokenByManagedIdentitySupplier.class);

    private ManagedIdentityParameters managedIdentityParameters;

//    AcquireTokenByManagedIdentitySupplier(ManagedIdentityParameters managedIdentityParameters) {
//        RequestContext requestContext = new RequestContext(clientApplication, managedIdentityParameters);
//        super(requestContext, )
//        this.managedIdentityParameters = managedIdentityParameters;
//
//    }

    AcquireTokenByManagedIdentitySupplier(AbstractClientApplicationBase abstractClientApplicationBase, MsalRequest msalRequest){
        super(abstractClientApplicationBase, msalRequest);
    }

    @Override
    AuthenticationResult execute() throws Exception {

        if (StringHelper.isNullOrBlank(managedIdentityParameters.resource))
        {
            throw new MsalClientException(
                    MsalError.ScopesRequired,
                    MsalErrorMessage.SCOPES_REQUIRED);
        }

        TokenRequestExecutor tokenRequestExecutor = new TokenRequestExecutor(
                clientApplication.authenticationAuthority,
                msalRequest,
                clientApplication.getServiceBundle()
        );

        if (!managedIdentityParameters.forceRefresh)
        {
            LOG.debug("ForceRefresh set to false. Attempting cache lookup");

//            AuthenticationResult authenticationResult = clientApplication.tokenCache.getCachedAuthenticationResult(
//                    clientApplication.authenticationAuthority.authority,
//                    managedIdentityParameters.resource,
//                    clientApplication.clientId(),
//                    silentRequest.assertion());

            AuthenticationResult authenticationResult = clientApplication.tokenCache.getCacheAuthenticationResult();

            if (authenticationResult == null) {
                throw new MsalClientException(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE, AuthenticationErrorCode.CACHE_MISS);
            }

            if (!StringHelper.isBlank(authenticationResult.accessToken())) {
                clientApplication.getServiceBundle().getServerSideTelemetry().incrementSilentSuccessfulCount();
            }

            if(authenticationResult != null) {
                return authenticationResult;
            }
        }
            LOG.info("Skipped looking for an Access Token in the cache because forceRefresh or Claims were set. ");

            // No AT in the cache
            AuthenticationResult authenticationResult = fetchNewAccessToken();
            clientApplication.tokenCache.saveTokens(tokenRequestExecutor,authenticationResult,clientApplication.authenticationAuthority.host);
            return authenticationResult;
        }

    private AuthenticationResult fetchNewAccessToken() {

        ManagedIdentityClient managedIdentityClient = new ManagedIdentityClient(msalRequest.requestContext());

        ManagedIdentityResponse managedIdentityResponse = managedIdentityClient
                .sendTokenRequest(managedIdentityParameters);

        return createFromManagedIdentityResponse(managedIdentityResponse);
    }

    private AuthenticationResult createFromManagedIdentityResponse(ManagedIdentityResponse managedIdentityResponse){
        long currTimestampSec = new Date().getTime() / 1000;
        long expiresOn = Long.valueOf(currTimestampSec + managedIdentityResponse.expiresOn);
        long refreshOn = expiresOn > 2*3600 ? (expiresOn/2) : 0L;

        return AuthenticationResult.builder()
                .accessToken(managedIdentityResponse.getAccessToken())
                .scopes(managedIdentityParameters.scopes().toString())
                .expiresOn(expiresOn)
                .extExpiresOn(0)
                .refreshOn(refreshOn)
                .scopes(managedIdentityParameters.getScopes().toString())
                .build();
    }
}
