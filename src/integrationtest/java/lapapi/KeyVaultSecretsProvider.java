//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//
//------------------------------------------------------------------------------

package lapapi;

import com.microsoft.aad.msal4j.AuthenticationResult;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IClientCredential;
import com.microsoft.aad.msal4j.TestConstants;
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
        AuthenticationResult result;
        try{
            ConfidentialClientApplication cca = new ConfidentialClientApplication.Builder(
                    CLIENT_ID, getClientCredentialFromKeyStore()).
                    authority(TestConstants.AUTHORITY_MICROSOFT).
                    build();
             result = cca.acquireTokenForClient(
                     Collections.singleton(TestConstants.KEYVAULT_DEFAULT_SCOPE)).
                     get();

        } catch(Exception e){
            throw new RuntimeException("Error acquiring token from Azure AD: " + e.getMessage());
        }
        if(result != null){
            return result.getAccessToken();
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
