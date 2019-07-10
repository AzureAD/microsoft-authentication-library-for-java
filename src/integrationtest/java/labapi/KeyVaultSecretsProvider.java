//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package labapi;

import com.microsoft.aad.msal4j.*;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

class KeyVaultSecretsProvider {

    private KeyVaultClient keyVaultClient;
    private final static String CLIENT_ID = "55e7e5af-ca53-482d-9aa3-5cb1cc8eecb5";
    private final static String CERTIFICATE_ALIAS = "JavaAutomationRunner";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";

    KeyVaultSecretsProvider(){
        keyVaultClient = getAuthenticatedKeyVaultClient();
    }

    String getSecret(String secretUrl){
        return keyVaultClient.getSecret(secretUrl).value();
    }

    private KeyVaultClient getAuthenticatedKeyVaultClient() {
        return new KeyVaultClient(
                new KeyVaultCredentials() {
            @Override
            public String doAuthenticate(String authorization, String resource, String scope) {
                return requestAccessTokenForAutomation();
            }
        });
    }

    private String requestAccessTokenForAutomation() {
        IAuthenticationResult result;
        try{
            ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                    CLIENT_ID, getClientCredentialFromKeyStore()).
                    authority(TestConstants.MICROSOFT_AUTHORITY).
                    build();

            result = cca.acquireToken(ClientCredentialParameters
                    .builder(Collections.singleton(TestConstants.KEYVAULT_DEFAULT_SCOPE))
                    .build()).
                    get();

        } catch(Exception e){
            throw new RuntimeException("Error acquiring token from Azure AD: " + e.getMessage());
        }
        if(result != null){
            return result.accessToken();
        } else {
            throw new NullPointerException("Authentication result is null");
        }
    }

    private IClientCredential getClientCredentialFromKeyStore() {
        PrivateKey key;
        X509Certificate publicCertificate;
        try {
            KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE, KEYSTORE_PROVIDER);
            keystore.load(null, null);

            key = (PrivateKey) keystore.getKey(CERTIFICATE_ALIAS, null);
            publicCertificate = (X509Certificate) keystore.getCertificate(
                    CERTIFICATE_ALIAS);
        } catch (Exception e){
            throw new RuntimeException("Error getting certificate from keystore: " + e.getMessage());
        }
        return ClientCredentialFactory.create(key, publicCertificate);
   }
}
