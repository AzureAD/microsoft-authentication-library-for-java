package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import lapapi.LabResponse;
import lapapi.LabUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class AuthorizationCodeIT {

    private final static Logger LOG = LoggerFactory.getLogger(AuthorizationCodeIT.class);

    private LabUserProvider labUserProvider;
    private static final String authority = "https://login.microsoftonline.com/organizations/";
    private static final String scopes = "https://graph.windows.net/.default";
    private String refreshToken;
    private PublicClientApplication pca;
    private String appId;



    @BeforeClass
    public void setUp() throws Exception {
        LabResponse labResponse = labUserProvider.getDefaultUser();
        appId = labResponse.getAppId();
        char[] password = labUserProvider.getUserPassword(labResponse.getUser()).toCharArray();
        pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(authority).
                build();
        AuthenticationResult result = pca.acquireTokenByUsernamePassword(
                scopes,
                labResponse.getUser().getUpn(),
                password.toString()).get();

        refreshToken = result.getRefreshToken();
    }


    @Test
    public void acquireTokenWithAuthorizationCode_ManagedUser(){

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv4_Federated(){

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv4_NotFederated(){

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv3_Federated(){

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv3_NotFederated(){

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2_Federated(){

    }

    @Test
    public void acquireTokenWithAuthorizationCode_ADFSv2_NotFederated(){

    }

//    private AuthorizationCode acquireAuthorizationCode(){
//        //String AuthCodeURL = buildAuthenticationCodeURL();
//
//    }

    private String buildAuthenticationCodeURL() throws UnsupportedEncodingException {
        String redirectUrl = authority  + "oauth2/v2.0/authorize?" +
                "response_type=code&" +
                "response_mode=form_post&" +
                "&client_id=" + appId +
                "&scope=" + URLEncoder.encode("openid offline_access profile " + scopes, "UTF-8");

        return redirectUrl;
    }
}
