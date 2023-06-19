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

    private final LabService labService;
    private Map<UserQueryParameters, User> userCache;

    private LabUserProvider() {
        labService = new LabService();
        userCache = new HashMap<>();
    }

    public static synchronized LabUserProvider getInstance() {
        if (instance == null) {
            instance = new LabUserProvider();
        }
        return instance;
    }

    public User getDefaultUser() {
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.AZURE_ENVIRONMENT, LabConstants.AZURE_ENVIRONMENT);

        return getLabUser(query);
    }

    public User getLabUser(UserQueryParameters userQuery) {
        if (userCache.containsKey(userQuery)) {
            return userCache.get(userQuery);
        }
        User response = labService.getUser(userQuery);
        userCache.put(userQuery, response);
        return response;
    }
}
