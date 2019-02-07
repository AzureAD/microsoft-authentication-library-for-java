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

import com.microsoft.aad.adal4j.AsymmetricKeyCredential;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.keyvault.authentication.KeyVaultCredentials;

import javax.naming.ServiceUnavailableException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class KeyVaultSecretsProvider {

    private KeyVaultClient keyVaultClient;
    private final static String CLIENT_ID = "55e7e5af-ca53-482d-9aa3-5cb1cc8eecb5";
    private final static String RESOURCE = "https://vault.azure.net";
    private final static String CERTIFICATE_ALIAS = "JavaAutomationRunner";
    private final static String KEYSTORE_TYPE = "Windows-MY";
    private final static String KEYSTORE_PROVIDER = "SunMSCAPI";
    private final static String AUTHORITY = "https://login.microsoftonline.com/microsoft.onmicrosoft.com";

    KeyVaultSecretsProvider(){
        keyVaultClient = getAuthenticatedKeyVaultClient();
    }

    String getLabUserPassword(String secretUrl){
        return keyVaultClient.getSecret(secretUrl).value();
    }

    private KeyVaultClient getAuthenticatedKeyVaultClient() {

        return new KeyVaultClient(new KeyVaultCredentials() {

            @Override
            public String doAuthenticate(String authorization, String resource, String scope) {
                String accessToken;
                try {
                    accessToken = requestAccessTokenForAutomation();
                } catch (Exception e){
                    throw new RuntimeException("Error getting access token for KeyVault: " +
                            e.getMessage());
                }
                return accessToken;
            }
        });
    }

    private String requestAccessTokenForAutomation()throws ServiceUnavailableException {
        ExecutorService service = null;
        AuthenticationContext context;
        AuthenticationResult result;

        try{
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext(AUTHORITY, false, service);
            AsymmetricKeyCredential credential = getClientCredentialFromKeyStore();

            Future<com.microsoft.aad.adal4j.AuthenticationResult> future =
                    context.acquireToken(RESOURCE, credential, null );
            result = future.get();
        } catch(Exception e){
            throw new RuntimeException("Error acquiring token from Azure AD: " + e.getMessage());
        } finally {
            service.shutdown();
        }

        if(result != null){
            return result.getAccessToken();
        } else {
            throw new ServiceUnavailableException("Authentication result is null");
        }
    }

    private AsymmetricKeyCredential getClientCredentialFromKeyStore() throws
            NoSuchProviderException, KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException{
        KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE, KEYSTORE_PROVIDER);
        keystore.load(null, null);

        PrivateKey key = (PrivateKey)keystore.getKey(CERTIFICATE_ALIAS, null);
        X509Certificate publicCertificate = (X509Certificate)keystore.getCertificate(
                CERTIFICATE_ALIAS);

        return AsymmetricKeyCredential.create(
                CLIENT_ID,
                key,
                publicCertificate);
    }
}
