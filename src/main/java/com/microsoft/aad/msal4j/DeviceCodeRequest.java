package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class DeviceCodeRequest extends MsalRequest {
    private final static Logger log = LoggerFactory.getLogger(DeviceCodeRequest.class);

    private AtomicReference<CompletableFuture<AuthenticationResult>> futureReference;
    private Consumer<DeviceCode> deviceCodeConsumer;
    private String scopes;

    DeviceCodeRequest(Consumer<DeviceCode> deviceCodeConsumer,
                      AtomicReference<CompletableFuture<AuthenticationResult>> futureReference,
                      Set<String> scopes,
                      ClientAuthentication clientAuthentication,
                      RequestContext requestContext){
        super(null, clientAuthentication, requestContext);
        this.scopes = String.join(" ", scopes);
        this.deviceCodeConsumer = deviceCodeConsumer;
        this.futureReference = futureReference;
    }

    DeviceCode acquireDeviceCode(String url,
                                 String clientId,
                                 Map<String, String> clientDataHeaders,
                                 ServiceBundle serviceBundle) throws Exception {

        Map<String, String> headers = new HashMap<>(clientDataHeaders);
        headers.put("Accept", "application/json");

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("client_id", clientId);

        String scopesParam = AbstractMsalAuthorizationGrant.COMMON_SCOPES_PARAM +
                AbstractMsalAuthorizationGrant.SCOPES_DELIMITER + scopes;

        queryParameters.put("scope", scopesParam);

        url = url + "?" + URLUtils.serializeParameters(queryParameters);
        final String json = HttpHelper.executeHttpGet(log, url, headers, serviceBundle);

        DeviceCode result;
        result = JsonHelper.convertJsonToObject(json, DeviceCode.class);

        result.setCorrelationId(headers.get(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME));
        result.setClientId(clientId);
        result.setScopes(scopes);

        return result;
    }

    void createAuthenticationGrant(DeviceCode deviceCode){
        setMsalAuthorizationGrant(new MsalDeviceCodeAuthorizationGrant(
                deviceCode, deviceCode.getScopes()));
    }

    AtomicReference<CompletableFuture<AuthenticationResult>> getFutureReference() {
        return futureReference;
    }

    Consumer<DeviceCode> getDeviceCodeConsumer() {
        return deviceCodeConsumer;
    }

    public String getScopes() {
        return scopes;
    }
}


