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
    private final Logger log = LoggerFactory.getLogger(DeviceCodeRequest.class);

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

        String urlWithQueryParams = createQueryParamsAndAppendToURL(url, clientId);
        Map<String, String> headers = appendToHeaders(clientDataHeaders);

        final String json = HttpHelper.executeHttpRequest(
                log,
                HttpMethod.GET,
                urlWithQueryParams,
                headers,
                null,
                this.getRequestContext(),
                serviceBundle);

        return parseJsonToDeviceCodeAndSetParameters(json, headers, clientId);
    }

    void createAuthenticationGrant(DeviceCode deviceCode){
        setMsalAuthorizationGrant(new DeviceCodeAuthorizationGrant(
                deviceCode, deviceCode.getScopes()));
    }

    private String createQueryParamsAndAppendToURL(String url, String clientId){
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("client_id", clientId);

        String scopesParam = MsalAuthorizationGrant.COMMON_SCOPES_PARAM +
                MsalAuthorizationGrant.SCOPES_DELIMITER + scopes;

        queryParameters.put("scope", scopesParam);

        url = url + "?" + URLUtils.serializeParameters(queryParameters);
        return url;
    }

    private  Map<String, String> appendToHeaders(Map<String, String> clientDataHeaders){
        Map<String, String> headers = new HashMap<>(clientDataHeaders);
        headers.put("Accept", "application/json");

        return headers;
    }

    private DeviceCode parseJsonToDeviceCodeAndSetParameters(
            String json,
            Map<String, String > headers,
            String clientId){

        DeviceCode result;
        result = JsonHelper.convertJsonToObject(json, DeviceCode.class);

        result.setCorrelationId(headers.get(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME));
        result.setClientId(clientId);
        result.setScopes(scopes);

        return result;
    }

    AtomicReference<CompletableFuture<AuthenticationResult>> getFutureReference() {
        return futureReference;
    }

    Consumer<DeviceCode> getDeviceCodeConsumer() {
        return deviceCodeConsumer;
    }

    String getScopes() {
        return scopes;
    }
}