// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.microsoft.aad.msal4j.*;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeyVaultSecretsProvider {

    private final KeyVaultClient keyVaultClient;
    private static final String CLIENT_ID = "55e7e5af-ca53-482d-9aa3-5cb1cc8eecb5";
    public static String CERTIFICATE_ALIAS = "MsalJavaAutomationRunner";

    private static final String WIN_KEYSTORE = "Windows-MY";
    private static final String KEYSTORE_PROVIDER = "SunMSCAPI";

    private static final String MAC_KEYSTORE = "KeychainStore";

    KeyVaultSecretsProvider(){
        keyVaultClient = getAuthenticatedKeyVaultClient();
    }

    static Map<String, String> cache = new ConcurrentHashMap<>();

    String getSecret(String secretUrl){
        if(cache.containsKey(secretUrl)){
            return cache.get(secretUrl);
        }
        String secret = keyVaultClient.getSecret(secretUrl).value();
        cache.put(secretUrl, secret);

        return secret;
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
            String os = System.getProperty("os.name");

            KeyStore keystore;
            if(os.toLowerCase().contains("windows")){
                keystore = KeyStore.getInstance(WIN_KEYSTORE, KEYSTORE_PROVIDER);
            }
            else{
                keystore = KeyStore.getInstance(MAC_KEYSTORE);
            }

            keystore.load(null, null);

            key = (PrivateKey) keystore.getKey(CERTIFICATE_ALIAS, null);
            publicCertificate = (X509Certificate) keystore.getCertificate(
                    CERTIFICATE_ALIAS);

        } catch (Exception e){
            throw new RuntimeException("Error getting certificate from keystore: " + e.getMessage());
        }
        return ClientCredentialFactory.createFromCertificate(key, publicCertificate);
   }
}
