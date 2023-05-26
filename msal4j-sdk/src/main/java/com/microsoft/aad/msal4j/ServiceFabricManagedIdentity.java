package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

class ServiceFabricManagedIdentity extends AbstractManagedIdentitySource{

    private final static Logger LOG = LoggerFactory.getLogger(ServiceFabricManagedIdentity.class);
    private static final String SERVICE_FABRIC_MSI_API_VERSION = "2019-07-01-preview";

    private static URI endpoint;

    private String identityHeaderValue;

    private ServiceFabricManagedIdentity(RequestContext requestContext, ServiceBundle serviceBundle, URI endpoint, String identityHeaderValue) {
        super(requestContext, serviceBundle, ManagedIdentitySourceType.ServiceFabric);
        this.endpoint = endpoint;
        this.identityHeaderValue = identityHeaderValue;
        if (isUserAssignedManagedIdentity())
        {
            LOG.warn(MsalErrorMessage.MANAGED_IDENTITY_USER_ASSIGNED_NOT_CONFIGURABLE_AT_RUNTIME);
        }
    }

    public static AbstractManagedIdentitySource create(RequestContext requestContext,
                                                       ServiceBundle serviceBundle)
    {
        String identityEndpoint = EnvironmentVariables.getIdentityEndpoint();
        String identityHeader = EnvironmentVariables.getIdentityHeader();
        String identityServerThumbprint = EnvironmentVariables.getIdentityServerThumbprint();

        if (StringHelper.isNullOrBlank(identityEndpoint) || StringHelper.isNullOrBlank(identityHeader) || StringHelper.isNullOrBlank(identityServerThumbprint))
        {
            LOG.info("[Managed Identity] Service Fabric managed identity unavailable.");
            return null;
        }

        try {
            endpoint = new URI(identityEndpoint);
        } catch (URISyntaxException e) {
            throw new MsalManagedIdentityException(MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT,
                    String.format(MsalErrorMessage.MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR,
                            "IDENTITY_ENDPOINT", identityEndpoint, "Service Fabric"),
                    ManagedIdentitySourceType.ServiceFabric);
        }

        LOG.info("[Managed Identity] Creating Service Fabric managed identity. Endpoint URI: " + identityEndpoint);
        return new ServiceFabricManagedIdentity(requestContext, serviceBundle, endpoint, identityHeader);
    }

    @Override
    public ManagedIdentityRequest createManagedIdentityRequest(String resource)
    {
        ManagedIdentityRequest request = new ManagedIdentityRequest(HttpMethod.GET, endpoint);

        Map<String, String> headers = new HashMap<>();
        headers.put("secret", identityHeaderValue);
        request.headers = headers;

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("api-version",SERVICE_FABRIC_MSI_API_VERSION );
        queryParameters.put("resource", resource);

        if (!StringHelper.isNullOrBlank(getManagedIdentityUserAssignedClientId()))
        {
            LOG.info("[Managed Identity] Adding user assigned client id to the request.");
            queryParameters.put(Constants.MANAGED_IDENTITY_CLIENT_ID,getManagedIdentityUserAssignedClientId());
        }

        if (!StringHelper.isNullOrBlank(getManagedIdentityUserAssignedResourceId()))
        {
            LOG.info("[Managed Identity] Adding user assigned resource id to the request.");
            queryParameters.put(Constants.MANAGED_IDENTITY_RESOURCE_ID,getManagedIdentityUserAssignedResourceId());
        }

        return request;
    }
}
