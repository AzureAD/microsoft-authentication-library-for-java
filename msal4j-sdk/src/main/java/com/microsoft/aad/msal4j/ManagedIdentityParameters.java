package com.microsoft.aad.msal4j;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
@Builder
public class ManagedIdentityParameters implements IAcquireTokenParameters{

    String resource;
    
    boolean forceRefresh;

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public boolean isForceRefresh() {
        return forceRefresh;
    }

    public void setForceRefresh(boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    @Override
    public Set<String> scopes() {
        return null;
    }

    @Override
    public ClaimsRequest claims() {
        return null;
    }

    @Override
    public Map<String, String> extraHttpHeaders() {
        return null;
    }

    @Override
    public String tenant() {
        return null;
    }

    @Override
    public Map<String, String> extraQueryParameters() {
        return null;
    }

    private static ManagedIdentityParametersBuilder builder() {

        return new ManagedIdentityParametersBuilder();
    }

    /**
     * Builder for {@link ManagedIdentityParameters}
     *
     * @param resource scopes application is requesting access to
     * @return builder that can be used to construct ManagedIdentityParameters
     */
    public static ManagedIdentityParametersBuilder builder(String resource) {

        return builder().resource(resource);
    }
}
