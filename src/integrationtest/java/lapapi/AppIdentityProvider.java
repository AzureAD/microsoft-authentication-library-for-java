package lapapi;

public class AppIdentityProvider {
    KeyVaultSecretsProvider keyVaultSecretsProvider;
    LabService labService;

    public AppIdentityProvider(){
        keyVaultSecretsProvider = new KeyVaultSecretsProvider();
        labService = new LabService();
    }

    public String getDefaultLabId(){
        return keyVaultSecretsProvider.getSecret(LabConstants.APP_ID_URL);
    }

    public String getDefaultLabPassword(){
        return keyVaultSecretsProvider.getSecret(LabConstants.APP_PASSWORD_URL);
    }

}
