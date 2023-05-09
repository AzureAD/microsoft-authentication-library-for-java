package com.microsoft.aad.msal4j;

import labapi.LabService;


import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;

public class MSIHttpClient implements IHttpClient{

    private String testWebServiceEndpoint;

    private DefaultHttpClient httpClient;

    public MSIHttpClient(String testWebServiceEndpoint){
        this.testWebServiceEndpoint = testWebServiceEndpoint;
    }

    @Override
    public IHttpResponse send(HttpRequest httpRequest) throws Exception {
        return null;
    }

    private HttpResponse execute(
            URI endpoint,
            Map<String, String> headers)
    {
        LabService labService = new LabService();
        //Get token for the MSIHelperService
        String token = LabService.getMSIToken();

        //Add the Authorization header
//        httpClient.DefaultRequestHeaders.Authorization =
//                new AuthenticationHeaderValue("Bearer", token);

        String encodedURL;
        //encode the URL before sending it to the helper service
        try {
            encodedURL = URLEncoder.encode(endpoint.toString(),"UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
//        var encodedUri = WebUtility.UrlEncode(endpoint.AbsoluteUri.ToLowerInvariant());

        //http get to the helper service
        HttpRequest requestMessage = null;

        //Pass the headers if any to the MSI Helper Service
        if (headers != null)
        {
            requestMessage = new HttpRequest(HttpMethod.GET, testWebServiceEndpoint+encodedURL, headers);
        }else{
            requestMessage = new HttpRequest(HttpMethod.GET, testWebServiceEndpoint + encodedURL);
        }

        //send the request to the helper service
        IHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.send(requestMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return (HttpResponse) httpResponse;
    }
}
