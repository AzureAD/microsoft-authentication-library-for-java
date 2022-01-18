package labapi;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecretIdentifier;
import com.microsoft.aad.msal4j.*;
import reactor.core.publisher.Mono;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KeyVaultSecretsProvider {

    private final SecretClient secretClient;

    private static final String CLIENT_ID = "55e7e5af-ca53-482d-9aa3-5cb1cc8eecb5";
    public static String CERTIFICATE_ALIAS = "MsalJavaAutomationRunner";

    private static final String WIN_KEYSTORE = "Windows-MY";
    private static final String KEYSTORE_PROVIDER = "SunMSCAPI";

    private static final String MAC_KEYSTORE = "KeychainStore";

    static Map<String, String> cache = new ConcurrentHashMap<>();

     KeyVaultSecretsProvider(){
        secretClient = getAuthenticatedSecretClient();
    }

    String getSecret(String secretUrl) {

        // extract keyName from secretUrl
        KeyVaultSecretIdentifier keyVaultSecretIdentifier = new KeyVaultSecretIdentifier(secretUrl);
        String key = keyVaultSecretIdentifier.getName();

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        String secret = secretClient.getSecret(key).getValue();
        cache.put(key, secret);

        return secret;
    }

    private SecretClient getAuthenticatedSecretClient(){

        SecretClient client = new SecretClientBuilder()
                .credential(getTokenCredential())
                .vaultUrl(TestConstants.MSIDLAB_VAULT_URL)
                .buildClient();

        return client;
    }

    private AccessToken requestAccessTokenForAutomation() {
        IAuthenticationResult result;
        try {
            ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                            CLIENT_ID, getClientCredentialFromKeyStore()).
                    authority(TestConstants.MICROSOFT_AUTHORITY).
                    build();
            result = cca.acquireToken(ClientCredentialParameters
                            .builder(Collections.singleton(TestConstants.KEYVAULT_DEFAULT_SCOPE))
                            .build()).
                    get();
        } catch (Exception e) {
            throw new RuntimeException("Error acquiring token from Azure AD: " + e.getMessage());
        }
        if (result != null) {
            return new AccessToken(result.accessToken(), OffsetDateTime.ofInstant(result.expiresOnDate().toInstant(), ZoneOffset.UTC));
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
            if (os.toLowerCase().contains("windows")) {
                keystore = KeyStore.getInstance(WIN_KEYSTORE, KEYSTORE_PROVIDER);
            } else {
                keystore = KeyStore.getInstance(MAC_KEYSTORE);
            }

            keystore.load(null, null);
            key = (PrivateKey) keystore.getKey(CERTIFICATE_ALIAS, null);
            publicCertificate = (X509Certificate) keystore.getCertificate(
                    CERTIFICATE_ALIAS);
        } catch (Exception e) {
            throw new RuntimeException("Error getting certificate from keystore: " + e.getMessage());
        }
        return ClientCredentialFactory.createFromCertificate(key, publicCertificate);
    }

    private TokenCredential getTokenCredential() {
        return tokenRequestContext -> Mono.defer(() -> Mono.just(requestAccessTokenForAutomation()));
    }
}
