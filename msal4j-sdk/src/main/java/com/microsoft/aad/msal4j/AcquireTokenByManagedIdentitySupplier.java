// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

class AcquireTokenByManagedIdentitySupplier extends AuthenticationResultSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenByManagedIdentitySupplier.class);

    private static final int TWO_HOURS = 2 * 3600;

    private ManagedIdentityParameters managedIdentityParameters;
    private final Supplier<ManagedIdentityMtlsBinding> mtlsBindingSupplier;

    AcquireTokenByManagedIdentitySupplier(ManagedIdentityApplication managedIdentityApplication, MsalRequest msalRequest) {
        this(managedIdentityApplication, msalRequest, null);
    }

    AcquireTokenByManagedIdentitySupplier(
            ManagedIdentityApplication managedIdentityApplication,
            MsalRequest msalRequest,
            Supplier<ManagedIdentityMtlsBinding> mtlsBindingSupplier) {
        super(managedIdentityApplication, msalRequest);
        this.managedIdentityParameters = (ManagedIdentityParameters) msalRequest.requestContext().apiParameters();
        this.mtlsBindingSupplier = mtlsBindingSupplier;
    }

    @Override
    AuthenticationResult execute() throws Exception {

        if (StringHelper.isNullOrBlank(managedIdentityParameters.resource)) {
            throw new MsalClientException(
                    MsalError.RESOURCE_REQUIRED_MANAGED_IDENTITY,
                    MsalErrorMessage.SCOPES_REQUIRED);
        }

        TokenRequestExecutor tokenRequestExecutor = new TokenRequestExecutor(
                clientApplication.authenticationAuthority,
                msalRequest,
                clientApplication.serviceBundle()
        );

        CacheRefreshReason cacheRefreshReason = CacheRefreshReason.NOT_APPLICABLE;
        ManagedIdentityMtlsBinding mtlsBinding = null;

        if (managedIdentityParameters.mtlsProofOfPossession()) {
            mtlsBinding = resolveMtlsBinding();
            MtlsBindingStrength requiredStrength =
                    managedIdentityParameters.attestationSupport()
                            ? MtlsBindingStrength.KEY_GUARD
                            : managedIdentityParameters.minimumBindingStrength();
            validateMinimumBindingStrength(mtlsBinding, requiredStrength);
            String extCacheKeyHash = managedIdentityParameters
                    .computeMtlsExtCacheKeyHash(
                            mtlsBinding.bindingContext().keyId());
            msalRequest.extCacheKeyHash(extCacheKeyHash);
        }

        if (managedIdentityParameters.forceRefresh) {
            LOG.debug("ForceRefresh set to true. Skipping cache lookup and attempting to acquire new token");
            return fetchNewAccessTokenAndSaveToCache(
                    tokenRequestExecutor,
                    CacheRefreshReason.FORCE_REFRESH,
                    mtlsBinding);
        }


        LOG.debug("ForceRefresh set to false. Attempting cache lookup");
        try {
            Set<String> scopes = new HashSet<>();
            scopes.add(this.managedIdentityParameters.resource);
            SilentParameters parameters = SilentParameters
                    .builder(scopes)
                    .tenant(managedIdentityParameters.tenant())
                    .claims(managedIdentityParameters.claims())
                    .build();

            RequestContext context = new RequestContext(
                    this.clientApplication,
                    PublicApi.ACQUIRE_TOKEN_SILENTLY,
                    parameters);

            SilentRequest silentRequest = new SilentRequest(
                    parameters,
                    this.clientApplication,
                    context,
                    null);
            silentRequest.extCacheKeyHash(msalRequest.extCacheKeyHash());

            AcquireTokenSilentSupplier supplier = new AcquireTokenSilentSupplier(
                    this.clientApplication,
                    silentRequest);

            AuthenticationResult result = supplier.execute();
            cacheRefreshReason = SilentRequestHelper.getCacheRefreshReasonIfApplicable(
                    parameters,
                    result,
                    LOG);

            // If the token does not need a refresh, return the cached token
            // Else refresh the token if it is either expired, proactively refreshable, or if the claims are passed.
            if (cacheRefreshReason == CacheRefreshReason.NOT_APPLICABLE) {
                LOG.debug("Returning token from cache");
                result.metadata().tokenSource(TokenSource.CACHE);
                return mtlsBinding == null
                        ? result
                        : result.withMtlsBindingContext(mtlsBinding.bindingContext());
            } else {
                if (cacheRefreshReason == CacheRefreshReason.CLAIMS) {
                    LOG.debug("Claims are passed, creating token hash and refreshing the token");
                    managedIdentityParameters.revokedTokenHash = StringHelper.createSha256HashHexString(result.accessToken());
                    return fetchNewAccessTokenAndSaveToCache(
                            tokenRequestExecutor,
                            CacheRefreshReason.CLAIMS,
                            mtlsBinding);
                }

                LOG.debug("Refreshing access token. Cache refresh reason: {}", cacheRefreshReason);
                return fetchNewAccessTokenAndSaveToCache(
                        tokenRequestExecutor,
                        cacheRefreshReason,
                        mtlsBinding);
            }
        } catch (MsalClientException ex) {
            if (ex.errorCode().equals(AuthenticationErrorCode.CACHE_MISS)) {
                LOG.debug("Cache lookup failed: {}", ex.getMessage());
                return fetchNewAccessTokenAndSaveToCache(
                        tokenRequestExecutor,
                        cacheRefreshReason,
                        mtlsBinding);
            } else {
                LOG.error("Error occurred while cache lookup: {}", ex.getMessage());
                throw ex;
            }
        }
    }

    private AuthenticationResult fetchNewAccessTokenAndSaveToCache(
            TokenRequestExecutor tokenRequestExecutor,
            CacheRefreshReason cacheRefreshReason,
            ManagedIdentityMtlsBinding mtlsBinding) {

        AuthenticationResult authenticationResult;
        if (mtlsBinding != null) {
            authenticationResult = acquireMtlsPopToken(
                    mtlsBinding,
                    tokenRequestExecutor);
        } else {
            ManagedIdentityClient managedIdentityClient =
                    new ManagedIdentityClient(msalRequest, tokenRequestExecutor.getServiceBundle());

            LOG.debug("[Managed Identity] Managed Identity source and ID type identified and set successfully, request will use Managed Identity for {}",
                    managedIdentityClient.managedIdentitySource.managedIdentitySourceType.name());

            ManagedIdentityResponse managedIdentityResponse = managedIdentityClient
                    .getManagedIdentityResponse(managedIdentityParameters);
            authenticationResult =
                    createFromManagedIdentityResponse(
                            managedIdentityResponse,
                            null);
        }

        clientApplication.tokenCache.saveTokens(tokenRequestExecutor, authenticationResult, clientApplication.authenticationAuthority.host);
        authenticationResult.metadata().tokenSource(TokenSource.IDENTITY_PROVIDER);
        authenticationResult.metadata().cacheRefreshReason(cacheRefreshReason);
        return authenticationResult;
    }

    private AuthenticationResult createFromManagedIdentityResponse(
            ManagedIdentityResponse managedIdentityResponse,
            ManagedIdentityMtlsBinding mtlsBinding) {
        long expiresOn = getExpiresOnFromManagedIdentityTimestamp(managedIdentityResponse.expiresOn);
        long refreshOn = calculateRefreshOn(expiresOn);
        AuthenticationResultMetadata metadata = AuthenticationResultMetadata.builder()
                .tokenSource(TokenSource.IDENTITY_PROVIDER)
                .refreshOn(refreshOn)
                .build();

        return AuthenticationResult.builder()
                .accessToken(managedIdentityResponse.getAccessToken())
                .scopes(managedIdentityParameters.resource())
                .expiresOn(expiresOn)
                .extExpiresOn(0)
                .refreshOn(refreshOn)
                .metadata(metadata)
                .tokenType(managedIdentityResponse.getTokenType())
                .isPopAuthorization(mtlsBinding == null ? null : Boolean.TRUE)
                .mtlsBindingContext(mtlsBinding == null ? null : mtlsBinding.bindingContext())
                .build();
    }

    private ManagedIdentityMtlsBinding resolveMtlsBinding() {
        if (mtlsBindingSupplier != null) {
            return mtlsBindingSupplier.get();
        }
        ManagedIdentityApplication application =
                (ManagedIdentityApplication) msalRequest.application();
        ManagedIdentityMtlsRequest request = createMtlsProviderRequest(
                application,
                msalRequest.requestContext(),
                managedIdentityParameters.attestationSupport());
        return getMtlsProviderBinding(
                ManagedIdentityMtlsProviderLoader.load(),
                request);
    }

    static ManagedIdentityMtlsRequest createMtlsProviderRequest(
            ManagedIdentityApplication application,
            RequestContext requestContext,
            boolean attestationEnabled) {
        ManagedIdentitySourceType source =
                ManagedIdentityClient.getManagedIdentitySource();
        if (source != ManagedIdentitySourceType.DEFAULT_TO_IMDS
                && source != ManagedIdentitySourceType.IMDS) {
            throw new MsalClientException(
                    "Managed identity mTLS PoP is supported only on the IMDS v2 VM/VMSS source.",
                    MsalError.MANAGED_IDENTITY_MTLS_UNSUPPORTED);
        }

        ManagedIdentityId identity = application.getManagedIdentityId();
        String queryName = null;
        String queryValue = identity.getUserAssignedId();
        switch (identity.getIdType()) {
            case CLIENT_ID:
                queryName = Constants.MANAGED_IDENTITY_CLIENT_ID;
                break;
            case RESOURCE_ID:
                queryName = Constants.MANAGED_IDENTITY_RESOURCE_ID_IMDS;
                break;
            case OBJECT_ID:
                queryName = Constants.MANAGED_IDENTITY_OBJECT_ID;
                break;
            case SYSTEM_ASSIGNED:
                queryValue = null;
                break;
            default:
                throw new MsalClientException(
                        "Unsupported managed identity selector for mTLS PoP.",
                        MsalError.MANAGED_IDENTITY_MTLS_UNSUPPORTED);
        }

        final ServiceBundle serviceBundle = application.serviceBundle();
        final HttpHelper imdsHttpHelper = new HttpHelper(
                application,
                new IMDSRetryPolicy());
        IManagedIdentityMtlsHttpClient httpClient = createMtlsProviderHttpClient(
                imdsHttpHelper,
                serviceBundle,
                requestContext);

        String bindingCacheKey = identity.getIdType().name() + ":"
                + (queryValue == null ? "" : queryValue)
                + (attestationEnabled
                        ? ":att1" : ":att0");
        return new ManagedIdentityMtlsRequest(
                queryName,
                queryValue,
                bindingCacheKey,
                requestContext.correlationId(),
                httpClient,
                attestationEnabled);
    }

    static ManagedIdentityMtlsBinding getMtlsProviderBinding(
            IManagedIdentityMtlsProvider provider,
            ManagedIdentityMtlsRequest request) {
        try {
            return provider.getOrCreateBinding(request);
        } catch (MsalException e) {
            throw e;
        } catch (RuntimeException e) {
            MsalClientException wrapped = new MsalClientException(
                    "The managed identity mTLS provider failed.",
                    MsalError.MANAGED_IDENTITY_MTLS_REQUEST_FAILED);
            wrapped.initCause(e);
            throw wrapped;
        }
    }

    static void validateMinimumBindingStrength(
            ManagedIdentityMtlsBinding binding,
            MtlsBindingStrength requiredStrength) {
        MtlsBindingStrength actualStrength =
                binding.bindingContext().bindingStrength();
        if (!actualStrength.meets(requiredStrength)) {
            throw new MsalClientException(
                    "The managed identity host produced mTLS binding strength "
                            + actualStrength + ", which does not meet the required "
                            + requiredStrength + " minimum.",
                    MsalError.MANAGED_IDENTITY_MTLS_MINIMUM_STRENGTH_NOT_MET);
        }
    }

    static IManagedIdentityMtlsHttpClient createMtlsProviderHttpClient(
            HttpHelper imdsHttpHelper,
            ServiceBundle serviceBundle,
            RequestContext requestContext) {
        return request -> {
            HttpMethod method;
            if ("GET".equalsIgnoreCase(request.method())) {
                method = HttpMethod.GET;
            } else if ("POST".equalsIgnoreCase(request.method())) {
                method = HttpMethod.POST;
            } else {
                throw new MsalClientException(
                        "Unsupported IMDS mTLS provider HTTP method: " + request.method(),
                        MsalError.MANAGED_IDENTITY_MTLS_REQUEST_FAILED);
            }

            HttpRequest httpRequest = new HttpRequest(
                    method,
                    request.url(),
                    request.headers(),
                    request.body());
            IHttpResponse response = imdsHttpHelper
                    .executeHttpRequest(httpRequest, requestContext, serviceBundle);
            return new ManagedIdentityMtlsHttpResponse(
                    response.statusCode(),
                    response.body(),
                    response.headers());
        };
    }

    private AuthenticationResult acquireMtlsPopToken(
            ManagedIdentityMtlsBinding binding,
            TokenRequestExecutor tokenRequestExecutor) {
        if (!(clientApplication.httpClient() instanceof IMtlsCapableHttpClient)) {
            throw new MsalClientException(
                    "The configured custom HTTP client does not declare support for request-specific mTLS. "
                            + "Implement IMtlsCapableHttpClient and honor HttpRequest.sslContext() "
                            + "or HttpRequest.sslSocketFactory().",
                    MsalError.MANAGED_IDENTITY_MTLS_HTTP_CLIENT_UNSUPPORTED);
        }

        String scope = managedIdentityParameters.resource().endsWith("/.default")
                ? managedIdentityParameters.resource()
                : managedIdentityParameters.resource().replaceAll("/+$", "") + "/.default";
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("client_id", binding.clientId());
        body.put("scope", scope);
        body.put("token_type", "mtls_pop");
        AuthenticationResult result;
        try {
            result = tokenRequestExecutor.executeTokenRequest(
                    new URL(binding.tokenEndpoint()),
                    binding.bindingContext().sslContext(),
                    body);
        } catch (MalformedURLException e) {
            throw new MsalClientException(
                    "The managed identity mTLS token endpoint is invalid.",
                    MsalError.MANAGED_IDENTITY_MTLS_REQUEST_FAILED);
        } catch (java.io.IOException e) {
            throw new MsalClientException(e);
        }

        validateMtlsTokenResponse(result);
        return result.withMtlsBindingContext(
                binding.bindingContext(),
                managedIdentityParameters.resource());
    }

    static void validateMtlsTokenResponse(
            IAuthenticationResult tokenResponse) {
        if (tokenResponse == null
                || StringHelper.isBlank(tokenResponse.accessToken())
                || !"mtls_pop".equals(tokenResponse.tokenType())) {
            throw new MsalServiceException(
                    "The managed identity mTLS endpoint did not explicitly return token_type=mtls_pop.",
                    MsalError.MANAGED_IDENTITY_MTLS_TOKEN_TYPE_INVALID,
                    ManagedIdentitySourceType.IMDS);
        }
    }

    static long getExpiresOnFromManagedIdentityTimestamp(String dateTimeStamp) {
        if (dateTimeStamp == null || dateTimeStamp.isEmpty()) {
            return 0;
        }

        // Try parsing as Unix timestamp (seconds since epoch)
        try {
            return Long.parseLong(dateTimeStamp);
        } catch (NumberFormatException e) {
            // Not a number
        }

        // Try parsing as ISO 8601
        try {
            return Instant.parse(dateTimeStamp).getEpochSecond();
        } catch (Exception e) {
            // Not ISO 8601
        }

        throw new MsalClientException(
                String.format("Failed to parse timestamp '%s'. Expected Unix epoch seconds or ISO 8601 format.",
                        dateTimeStamp),
                AuthenticationErrorCode.INVALID_TIMESTAMP_FORMAT);
    }

    private long calculateRefreshOn(long expiresOn) {
        long timestampSeconds = System.currentTimeMillis() / 1000;
        long expiresIn = expiresOn - timestampSeconds;

        //The refreshOn value should be half the value of the token lifetime, if the lifetime is greater than two hours
        return expiresIn > TWO_HOURS ? (expiresIn / 2) + timestampSeconds : 0;
    }
}
