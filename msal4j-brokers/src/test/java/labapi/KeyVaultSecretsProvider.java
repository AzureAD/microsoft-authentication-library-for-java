package labapi;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecretIdentifier;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;
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

    public final static String CLIENT_ID = "f62c5ae3-bf3a-4af5-afa8-a68b800396e9";
    public static String CERTIFICATE_ALIAS = "LabAuth.MSIDLab.com";

    private static final String WIN_KEYSTORE = "Windows-MY";
    private static final String KEYSTORE_PROVIDER = "SunMSCAPI";

    private static final String MAC_KEYSTORE = "KeychainStore";

    static Map<String, String> cache = new ConcurrentHashMap<>();

    KeyVaultSecretsProvider() {
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

    private SecretClient getAuthenticatedSecretClient() {
        return new SecretClientBuilder()
                .credential(getTokenCredential())
                .vaultUrl(LabConstants.MSIDLAB_VAULT_URL)
                .buildClient();
    }

    private AccessToken requestAccessTokenForAutomation() {
        IAuthenticationResult result;
        try {
            ConfidentialClientApplication cca =
                    ConfidentialClientApplication
                            .builder(CLIENT_ID, getClientCredentialFromKeyStore())
                            .authority(LabConstants.MICROSOFT_AUTHORITY).sendX5c(true)
                            .build();
            result = cca.acquireToken(ClientCredentialParameters
                                              .builder(Collections.singleton(
                                                      LabConstants.KEYVAULT_DEFAULT_SCOPE))
                                              .build())
                             .get();
        } catch (Exception e) {
            throw new RuntimeException("Error acquiring token from Azure AD: " + e.getMessage());
        }
        if (result != null) {
            return new AccessToken(
                    result.accessToken(),
                    OffsetDateTime.ofInstant(result.expiresOnDate().toInstant(), ZoneOffset.UTC));
        } else {
            throw new NullPointerException("Authentication result is null");
        }
    }

    public IClientCredential getClientCredentialFromKeyStore() {
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
            key = (PrivateKey)keystore.getKey(CERTIFICATE_ALIAS, null);
            publicCertificate = (X509Certificate)keystore.getCertificate(CERTIFICATE_ALIAS);
        } catch (Exception e) {
            throw new RuntimeException("Error getting certificate from keystore: " + e.getMessage());
        }
        return ClientCredentialFactory.createFromCertificate(key, publicCertificate);
    }

    private TokenCredential getTokenCredential() {
        return tokenRequestContext -> Mono.defer(() -> Mono.just(requestAccessTokenForAutomation()));
    }
}
