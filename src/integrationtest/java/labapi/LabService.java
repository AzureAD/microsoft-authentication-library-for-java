//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
//
//------------------------------------------------------------------------------

package labapi;

import com.google.gson.Gson;
import org.testng.util.Strings;

import java.util.HashMap;
import java.util.Map;

class LabService {

    LabResponse getLabResponse(UserQuery query) throws LabUserNotFoundException{
        String result;
        try {
            Map<String, String> queryMap = createLabQuery(query);
            result = HttpClientHelper.sendRequestToLab(queryMap, query.useBetaEndpoint());
        } catch(Exception ex){
            throw new RuntimeException("Error sending request to lab: " +
                    ex.getMessage());
        }
        if(Strings.isNullOrEmpty(result)){
            throw new LabUserNotFoundException(query,
                    "Lab response is null or empty. No lab user with specified parameter exists");
        }
        return deserializeLabResponse(result);
    }

    private Map<String, String> createLabQuery(UserQuery query) {
        Map<String, String> queryMap = new HashMap<String, String>();

        queryMap.put(LabConstants.MOBILE_APP_MANAGEMENT_WITH_CONDITIONAL_ACCESS,
                LabConstants.FALSE);
        queryMap.put(LabConstants.MOBILE_DEVICE_MANAGEMENT_WITH_CONDITIONAL_ACCESS,
                LabConstants.FALSE);

        queryMap.put(LabConstants.MOBILE_APP_MANAGEMENT, (query.isMamUser()) ?
                LabConstants.TRUE :
                LabConstants.FALSE);

        queryMap.put(LabConstants.EXTERNAL, (query.isExternalUser()) ?
                LabConstants.TRUE :
                LabConstants.FALSE);

        queryMap.put(LabConstants.MULTIFACTOR_AUTHENTICATION, (query.isMfaUser()) ?
                LabConstants.TRUE :
                LabConstants.FALSE);

        queryMap.put(LabConstants.FEDERATED_USER, (query.isFederatedUser() ?
                LabConstants.TRUE :
                LabConstants.FALSE));

        if (query.getFederationProvider() != null) {
            queryMap.put(LabConstants.FEDERATION_PROVIDER, query.getFederationProvider().toString());
        }

        if (query.getLicenses() != null && !query.getLicenses().isEmpty()) {
            queryMap.put(LabConstants.LICENSE, query.getLicenses().toArray().toString());
        }

        if (query.getUserType() != null) {
            queryMap.put(LabConstants.USERTYPE, query.getUserType().toString());
        }

        if (query.getB2CIdentityProvider() != null) {
            switch (query.getB2CIdentityProvider()) {
                case LOCAL:
                    queryMap.put(LabConstants.B2C_PROVIDER, LabConstants.B2C_LOCAL);
                    break;
                case GOOGLE:
                    queryMap.put(LabConstants.B2C_PROVIDER, LabConstants.B2C_GOOGLE);
                    break;
                case FACEBOOK:
                    queryMap.put(LabConstants.B2C_PROVIDER, LabConstants.B2C_FACEBOOK);
                    break;
            }
        }
        return queryMap;
    }

    private LabResponse deserializeLabResponse(String result){
        Gson gson = new Gson();
        LabResponse labResponse = gson.fromJson(result, LabResponse.class);
        LabUser labUser = labResponse.getUser();

        if(labUser.getHomeTenantId() != null && labUser.getHomeUpn() != null){
            labUser.initializeHomeUser();
        }
        return labResponse;
    }
}
