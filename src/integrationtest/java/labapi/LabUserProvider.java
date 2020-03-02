//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package labapi;

import java.util.HashMap;
import java.util.Map;

public class LabUserProvider {

    private static LabUserProvider instance;

    private final KeyVaultSecretsProvider keyVaultSecretsProvider;
    private final LabService labService;
    private Map<UserQueryParameters, User> userCache;

    private LabUserProvider(){
        keyVaultSecretsProvider = new KeyVaultSecretsProvider();
        labService = new LabService();
        userCache = new HashMap<>();
    }

    public static synchronized LabUserProvider getInstance(){
        if(instance == null){
            instance = new LabUserProvider();
        }
        return instance;
    }

    public User getDefaultUser() {
        return getDefaultUser(AzureEnvironment.AZURE);
    }

    public User getDefaultUser(String azureEnvironment) {

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.AZURE_ENVIRONMENT, azureEnvironment);

        return getLabUser(query);
    }

    public User getFederatedAdfsUser(String federationProvider){
        return getFederatedAdfsUser(AzureEnvironment.AZURE, federationProvider);
    }

    public User getFederatedAdfsUser(String azureEnvironment, String federationProvider){

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.AZURE_ENVIRONMENT, azureEnvironment);
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER,  federationProvider);
        query.parameters.put(UserQueryParameters.USER_TYPE,  UserType.FEDERATED);

        return getLabUser(query);
    }

    public User getOnPremAdfsUser(String federationProvider){
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER, federationProvider);
        query.parameters.put(UserQueryParameters.USER_TYPE,  UserType.ON_PREM);

        return getLabUser(query);
    }

    public User getB2cUser(String b2cProvider) {
        return getB2cUser(AzureEnvironment.AZURE, b2cProvider);
    }

    public User getB2cUser(String azureEnvironment, String b2cProvider) {
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.AZURE_ENVIRONMENT,  azureEnvironment);
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.B2C);
        query.parameters.put(UserQueryParameters.B2C_PROVIDER, b2cProvider);

        return getLabUser(query);
    }

    public User getUserByAzureEnvironment(String azureEnvironment) {

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.AZURE_ENVIRONMENT,  azureEnvironment);

        return getLabUser(query);
    }

    public User getLabUser(UserQueryParameters userQuery){
        if(userCache.containsKey(userQuery)){
            return userCache.get(userQuery);
        }
        User response = labService.getUser(userQuery);
        userCache.put(userQuery, response);
        return response;
    }
}
