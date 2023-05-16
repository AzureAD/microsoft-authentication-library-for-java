package com.microsoft.aad.msal4j;

import labapi.LabService;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MSIHttpClient implements IHttpClient{

    private DefaultHttpClient httpClient;
    private String testWebServiceEndpoint;
    public int DEFAULT_CONNECT_TIMEOUT = 10000;
    public int DEFAULT_READ_TIMEOUT = 15000;

    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int readTimeout = DEFAULT_READ_TIMEOUT;
    public MSIHttpClient(String testWebServiceEndpoint){
        this.testWebServiceEndpoint = testWebServiceEndpoint;
        this.httpClient = new DefaultHttpClient(null, null, null, null);
    }

    @Override
    public IHttpResponse send(HttpRequest httpRequest) throws Exception {

        return httpClient.send(httpRequest);
    }

//    private HttpResponse execute(
//            URI endpoint,
//            Map<String, String> headers)
//    {
//        LabService labService = new LabService();
//        //Get token for the MSIHelperService
//        String token = LabService.getMSIToken();
//
//        String encodedURL;
//        //encode the URL before sending it to the helper service
//        try {
//            encodedURL = URLEncoder.encode(endpoint.toString(),"UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
////        var encodedUri = WebUtility.UrlEncode(endpoint.AbsoluteUri.ToLowerInvariant());
//
//        //http get to the helper service
//        HttpRequest requestMessage = null;
//        Map<String, String> httpHeaders;
//        if(headers.isEmpty()){
//            httpHeaders = new HashMap<>();
//        }else{
//            httpHeaders = headers;
//        }
//        //Add the Authorization header
//        httpHeaders.put("Authorization", "Bearer " + token);
//        //Pass the headers if any to the MSI Helper Service
//        if (headers != null)
//        {
//            requestMessage = new HttpRequest(HttpMethod.GET, testWebServiceEndpoint+encodedURL, httpHeaders);
//        }else{
//            requestMessage = new HttpRequest(HttpMethod.GET, testWebServiceEndpoint + encodedURL);
//        }
//
//        //send the request to the helper service
//        IHttpResponse httpResponse = null;
//        try {
//            httpResponse = httpClient.send(requestMessage);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        return (HttpResponse) httpResponse;
//    }
}
