// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class MSIHelperService {

    private static final Logger log = LoggerFactory.getLogger(MSIHelperService.class);
    
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String ContentTypeTextOrHtml = "text/html";
    private static final String ContentTypeMulipartOrFormData = "multipart/form-data";

    //default azure resource if nothing is passed in the controllers
    private static final String DefaultAzureResource = "WebApp";

    //IDENTITY_HEADER in the App Service
    private static final String ManagedIdentityAuthenticationHeader = "X-IDENTITY-HEADER";

    //Environment variables
    private static final String s_requestAppID = System.getenv("requestAppID");
    private static final String s_requestAppSecret = System.getenv("requestAppSecret");
    private static final String s_functionAppUri = System.getenv("functionAppUri");
    private static final String s_functionAppEnvCode = System.getenv("functionAppEnvCode");
    private static final String s_functionAppMSICode = System.getenv("functionAppMSICode");
    private static final String s_vmWebhookLocation = System.getenv("webhookLocation");
    private static final String s_azureArcWebhookLocation = System.getenv("AzureArcWebHookLocation");
    private static final String s_oMSAdminClientID = System.getenv("OMSAdminClientID");
    private static final String s_oMSAdminClientSecret = System.getenv("OMSAdminClientSecret");

    //Microsoft authority endpoint
    private static final String authority = "https://login.microsoftonline.com/72f988bf-86f1-41af-91ab-2d7cd011db47";

    //OMS Runbook 
    private static final String LabSubscription = "https://management.azure.com/subscriptions/c1686c51-b717-4fe0-9af3-24a20a41fb0c/";
    private static final String RunbookLocation = "resourceGroups/OperationsManagementSuite/";
    private static final String RunbookJobProvider = "providers/Microsoft.Automation/automationAccounts/OMSAdmin/jobs/";
    private static final String AzureRunbook = LabSubscription + RunbookLocation + RunbookJobProvider;
    private static final String RunbookAPIVersion = "2019-06-01";

    //Azure Resources
    enum AzureResource
    {
        WebApp,
        Function,
        VM,
        AzureArc,
        ServiceFabric,
        CloudShell
    }

    
     /** Gets the Environment Variables from the Azure Web App
      <param name="logger"></param>
     <returns>Returns the environment variables</returns> */
    public static Map<String, String> getWebAppEnvironmentVariables()
    {
        //Gets Azure Web App Specific environment variables and sends it back
        //Sending back the specific ones that is needed for the MSI tests
        Map<String, String> keyValuePairs = new HashMap<>();
        keyValuePairs.put("IDENTITY_HEADER", System.getenv("IDENTITY_HEADER"));
        keyValuePairs.put("IDENTITY_ENDPOINT", System.getenv("IDENTITY_ENDPOINT"));
        keyValuePairs.put("IDENTITY_API_VERSION", System.getenv("IDENTITY_API_VERSION"));

        log.info("GetWebAppEnvironmentVariables Function called.");

        return keyValuePairs;
    }
    
    /** Gets the Environment Variable from the Azure Function
     <param name="httpClient"></param>
     <param name="logger"></param>
      <returns>Returns the environment variables</returns> */
    public static  Map<String, String> getFunctionAppEnvironmentVariables(
            IHttpClient httpClient)
    {
        log.info("GetFunctionAppEnvironmentVariables Function called.");

        String scope = "https://request.msidlab.com/.default";
        String token =  getMSALToken(s_requestAppID, s_requestAppSecret, Collections.singleton(scope));

        String url = s_functionAppUri + "GetEnvironmentVariablescode="
                + s_functionAppEnvCode;

        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, url, getAuthorizationHeader(token));

        IHttpResponse httpResponse;
        //send the request
        try {
            httpResponse = httpClient.send(httpRequest);
            String content =  httpResponse.body();
            Map<String, String> envValuePairs = JsonHelper.convertJsonToObject(content, Map.class);
            log.info("getFunctionAppEnvironmentVariables call was successful.");
            return envValuePairs;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
     /** Gets the Environment Variables for IMDS
     <param name="logger"></param>
     <returns>Returns the environment variable</returns> */
    public static Map<String, String> getVirtualMachineEnvironmentVariables()
    {
        //IMDS endpoint has only one environment variable and the VMs do not have this
        //MSAL .Net has this value hardcoded for now. So sending a set value

        Map<String, String> keyValuePairs = new HashMap<>();
        keyValuePairs.put("AZURE_POD_IDENTITY_AUTHORITY_HOST", "http://169.254.169.254/metadata/identity/oauth2/token");
        keyValuePairs.put("IMDS_API_VERSION", "2018-02-01");

        log.info("getVirtualMachineEnvironmentVariables Function called.");
        return keyValuePairs;
    }

    
    /** Gets the Environment Variables for Azure ARC
     <param name="logger"></param>
     <returns>Returns the environment variable</returns> */
    public static Map<String, String> getAzureArcEnvironmentVariables()
    {
        //IMDS endpoint has only one environment variable and the VMs do not have this
        //MSAL .Net has this value hardcoded for now. So sending a set value

        Map<String, String> keyValuePairs = new HashMap<>();
        keyValuePairs.put("IDENTITY_ENDPOINT", "http://localhost:40342/metadata/identity/oauth2/token");
        keyValuePairs.put("IMDS_ENDPOINT", "http://localhost:40342/metadata/identity/oauth2/token");
        keyValuePairs.put("API_VERSION", "2020-06-01");

        log.info("GetAzureArcEnvironmentVariables Function called.");

        return keyValuePairs;
    }

    
    /**  Gets the MSI Token from the Azure Web App
    
     <param name="identityHeader"></param>
     <param name="uri"></param>
     <param name="httpClient"></param>
     <param name="logger"></param>
     <returns>Returns MSI Token</returns> */
    public static IHttpResponse getWebAppMSIToken(
            String identityHeader,
            String uri,
            IHttpClient httpClient)
    {
        log.info("getWebAppMSIToken Function called.");

        String decodedUri = URLEncoder.encode(uri);

        //set the http get method and the required headers for a web app
        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, decodedUri);
        Map<String, String> headers = new HashMap<>();
        headers.put(ManagedIdentityAuthenticationHeader, identityHeader);

        //send the request
        IHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.send(httpRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("getWebAppMSIToken Function call was successful.");
        return httpResponse;
    }

    
     /**Gets the MSI Token from the Azure Function App
    
     <param name="identityHeader"></param>
     <param name="uri"></param>
     <param name="httpClient"></param>
     <param name="logger"></param>
     <returns>Returns MSI Token</returns> */
    public static IHttpResponse getFunctionAppMSIToken(
            String identityHeader,
            String uri,
            IHttpClient httpClient)
    {
        log.info("GetFunctionAppMSIToken Function called.");

        //Scopes
        Set<String> scopes = Collections.singleton("https://request.msidlab.com/.default");

        String token =  getMSALToken(s_requestAppID, s_requestAppSecret, scopes);

        //send the request
        String encodedUri = URLEncoder.encode(uri);

        String finalUri = s_functionAppUri + "getTokencode=" + s_functionAppMSICode +
                "&uri=" + encodedUri + "&headers=" +identityHeader ;

        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, finalUri , getAuthorizationHeader(token));

        IHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.send(httpRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("getFunctionAppMSIToken call was successful.");

        return httpResponse;
    }

    
    /** Gets the MSI Token from the Azure Virtual Machine
     <param name="identityHeader"></param>
     <param name="uri"></param>
     <param name="httpClient"></param>
     <param name="logger"></param>
     <returns></returns> */
    public static ContentResult getVirtualMachineMSIToken(
            String identityHeader,
            String uri,
            IHttpClient httpClient
            )
    {
        log.info("GetVirtualMachineMSIToken Function called.");
        String response;
        IHttpResponse responseMessage = new HttpResponse();

        try
        {
            //Scopes
            Set<String> scopes = Collections.singleton( "https://management.core.windows.net/.default");

            //get the msal token for the client
            String token =  getMSALToken(
                s_oMSAdminClientID,
                s_oMSAdminClientSecret,
                scopes);

            //Set the Authorization header
            getAuthorizationHeader(token);
            Map<String, String> headers = new HashMap<>();
            headers.put("Bearer", token);
            //Set additional headers
            headers.put("MSI_Identity_Header", identityHeader);
            headers.put("MSI_URI", uri.toString());

            //get the job id
            String jobId =  startAzureRunbookandGetJobId(httpClient, AzureResource.VM);

            if ( isAzureRunbookJobStatusCompleted(jobId, httpClient))
            {
                String runbookUri = AzureRunbook +
                        jobId +
                        "/outputapi-version=" +
                        RunbookAPIVersion;
                HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, runbookUri );
                responseMessage =  httpClient.send(httpRequest);
                
                //send back the response
                response =  responseMessage.body();

                log.info("GetVirtualMachineMSIToken call was successful.");
            }
                else
            {
                log.error("Runbook failed to get MSI Token.");

                //Runbook failed
                response = "Azure Runbook failed to execute.";
            }

            return getContentResult(response, "application/json", responseMessage.statusCode());
        }
        catch (Exception ex)
        {
            //Catch the Azure Runbook exception
            String errorResponse = ex.getMessage();
            log.error("GetVirtualMachineMSIToken call failed.");
            return getContentResult(errorResponse, "application/json", responseMessage.statusCode());
        }
    }

    
    /** Gets the MSI Token from the Azure Arc Machine
    
     <param name="identityHeader"></param>
     <param name="uri"></param>
     <param name="httpClient"></param>
     <param name="logger"></param>
     <returns></returns> */
    public static ContentResult getAzureArcMSIToken(
            String identityHeader,
            String uri,
            IHttpClient httpClient)
    {
        log.info("GetAzureArcMSIToken Function called.");
        String response;
        IHttpResponse responseMessage = new HttpResponse();

        try
        {
            //Scopes
            Set<String> scopes = Collections.singleton( "https://management.core.windows.net/.default");

            //get the msal token for the client
            String token =  getMSALToken(
                s_oMSAdminClientID,
                s_oMSAdminClientSecret,
                scopes);

            Map<String, String> headers = new HashMap<>();
            //set the authorization header

            //Set additional headers
            headers.put("MSI_Identity_Header", identityHeader);
            headers.put("MSI_URI", uri.toString());
            //Set the Authorization header
            headers.put("Bearer", token);

            //get the job id
            String jobId =  startAzureRunbookandGetJobId(httpClient,AzureResource.AzureArc);

            HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, AzureRunbook +
                    jobId +
                    "/outputapi-version=" +
                    RunbookAPIVersion , headers);
            if (isAzureRunbookJobStatusCompleted(jobId, httpClient))
            {
                responseMessage = httpClient.send(
                    httpRequest
                );
                //send back the response
                response =  responseMessage.body();
                log.info("GetAzureArcMSIToken call was successful.");
            } else {
                log.error("Runbook failed to get MSI Token.");

                //Runbook failed
                response = "Azure Runbook failed to execute.";
            }

            return getContentResult(response, "application/json", responseMessage.statusCode());
        }
        catch (Exception ex)
        {
            //Catch the Azure Runbook exception
            String errorResponse = ex.getMessage();
            log.error("getAzureArcMSIToken call failed.");
            return getContentResult(errorResponse, "application/json", responseMessage.statusCode());
        }
    }

    
     /** Get Azure Runbook Job Status
    
     <param name="jobId"></param>
     <param name="httpClient"></param>
     <param name="logger"></param>
     <returns>Azure runbook job Status</returns> */
    private static  boolean isAzureRunbookJobStatusCompleted(
            String jobId,
            IHttpClient httpClient
            )
    {
        log.info("AzureRunbookJobStatusIsCompleted Function called.");
        //Get the Job status
        IHttpResponse jobStatusResponse;
//        RunBookJobStatus runBookJobStatus;
        String currentJobStatus;
        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, AzureRunbook +
                jobId +
                "api-version=" +
                RunbookAPIVersion);

        do
        {
            //get the current job status based on the job id
            try {
                jobStatusResponse =  httpClient.send(httpRequest);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //get the status
            Map<String, Object> runBookJobStatus =  JsonHelper.convertJsonToObject(jobStatusResponse.body(), Map.class);

//            currentJobStatus = runBookJobStatus.Properties.Status;
            currentJobStatus = (String)runBookJobStatus.get("properties");
            //catch runbook failure
            if (currentJobStatus.equals("Failed"))
            {
                return false;
            }

            log.info("Current Job Status is - { currentJobStatus }.");
        }
        while (currentJobStatus != "Completed");

        return true;
    }


     /** Starts the Runbook and gets Azure Runbook Job Id
     <param name="httpClient"></param>
     <param name="logger"></param>
     <param name="azureResource"></param>
     <returns>Azure runbook job ID</returns> */
    private static  String startAzureRunbookandGetJobId(IHttpClient httpClient, AzureResource azureResource)
    {
        log.info("StartAzureRunbookandGetJobId Function called.");

        String payload = "";

//        String content = new String(payload, Encoding"application/json");

        String webHookLocation;
        if (azureResource == null || azureResource == AzureResource.VM)
        {
            webHookLocation = s_vmWebhookLocation;
        }
        else
        {
            webHookLocation = s_azureArcWebhookLocation;
        }

        HttpRequest httpRequest = new HttpRequest(HttpMethod.POST, webHookLocation, payload);
        //invoke the azure runbook from the webhook

        IHttpResponse invokeWebHook = null;
        try {
            invokeWebHook = httpClient.send(httpRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Get the Job ID

        Map<String, Object> responseMap = JsonHelper.convertJsonToObject(invokeWebHook.body(), Map.class);
        String[] jobIds = (String[])responseMap.get("JobIDs");
        String jobId = jobIds[0];
//
//        WebHookResponse jobResponse =  invokeWebHook.Content
//            .ReadFromJson<WebHookResponse>();

//        String jobId = jobResponse.JobIDs[0];

        if (jobIds!=null && jobIds.length!=0 && !StringHelper.isNullOrBlank(jobIds[0])) {
            log.info("Job ID retrieved from the Azure Runbook.");
            log.info("Job Id is - { jobId }.");
        } else {
            log.error("Failed to get Job ID from the Azure Runbook.");
        }

        return jobId;
    }
    
    /** Get the Client Token
     <param name="clientID"></param>
     <param name="secret"></param>
     <param name="scopes"></param>
     <param name="logger"></param>
     <returns></returns> */
    private static  String getMSALToken(
            String clientID,
            String secret,
            Set<String> scopes
            )
    {
        log.info("getMSALToken Function called.");

        IClientCredential credential = ClientCredentialFactory.createFromSecret(secret);

        //Confidential Client Application Builder
        ConfidentialClientApplication cca = null;
        try {
            cca = ConfidentialClientApplication.builder(clientID, credential)
                    .authority(authority)
                    .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        //Acquire Token For Client using MSAL
        try
        {
            IAuthenticationResult result = cca
                    .acquireToken(ClientCredentialParameters.builder(scopes)
                            .build()).get();
            log.info("MSAL Token acquired successfully.");
            log.info("MSAL Token source is : { result.AuthenticationResultMetadata.TokenSource }");

            return result.accessToken();
        }
        catch (MsalException | InterruptedException | ExecutionException ex)
        {
            log.error(ex.getMessage());
            return ex.getMessage();
        }
    }
    
    /** Sets the authorization header on the http client
    
     <param name="token"></param>
     <param name="httpClient"></param>
     <param name="logger"></param>
     <returns></returns> */
    private static Map<String, String> getAuthorizationHeader(
            String token) {
        log.info("getAuthorizationHeader Function called.");
        Map<String, String> headers = new HashMap<>();
        headers.put("Bearer", token);
        return headers;
    }

    
     /** Returns Content Result for final output from the web api
    
     <param name="content"></param>
     <param name="contentEncoding"></param>
     <param name="statusCode"></param>
     <returns></returns> */
    private static ContentResult getContentResult(
            String content,
            String contentEncoding,
            int statusCode)
    {
        return new ContentResult(
            content,
            contentEncoding,
            statusCode
        );
    }
}
