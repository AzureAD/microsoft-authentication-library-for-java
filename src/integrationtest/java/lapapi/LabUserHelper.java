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

import com.sun.javaws.exceptions.InvalidArgumentException;
import org.testng.util.Strings;
import sun.plugin.dom.exception.InvalidStateException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LabUserHelper {
    private static KeyVaultSecretsProvider keyVaultSecretsProvider = new KeyVaultSecretsProvider();
    private static LabServiceApi labService = new LabServiceApi(keyVaultSecretsProvider);
    private static Map<UserQuery, LabResponse> userCache = new HashMap<>();

    public static LabResponse getLabUserData(UserQuery userQuery) throws IOException {
        if(userCache.containsKey(userQuery)){
            return userCache.get(userQuery);
        }

        LabResponse user = labService.getLabResponseFromApi(userQuery);
        if(user == null){
            throw new LabUserNotFoundException(userQuery, "Found no users for the given query.");
        }

        userCache.put(userQuery, user);
        return user;
    }

    public static LabResponse getDefaultUser() throws IOException{
        UserQuery query =  new UserQuery.Builder().
                isMamUser(false).
                isMfaUser(false).
                isFederatedUser(false).
                build();
        return getLabUserData(query);
    }

    public static LabResponse getB2cLocalAccount() throws IOException{
        UserQuery query = new UserQuery.Builder().
                userType(UserType.B2C).
                b2CIdentityProvider(B2CIdentityProvider.LOCAL).
                build();
        return getLabUserData(query);
    }

    public static LabResponse getB2cFacebookAccount() throws IOException{
        UserQuery query = new UserQuery.Builder().
                userType(UserType.B2C).
                b2CIdentityProvider(B2CIdentityProvider.FACEBOOK).
                build();
        return getLabUserData(query);
    }

    public static LabResponse getB2cGoogleAccount() throws IOException{
        UserQuery query = new UserQuery.Builder().
                userType(UserType.B2C).
                b2CIdentityProvider(B2CIdentityProvider.GOOGLE).
                build();
        return getLabUserData(query);
    }

    public static LabResponse getAdfsUser(FederationProvider federationProvider, boolean federated)
            throws IOException{
        UserQuery query = new UserQuery.Builder().
                isMamUser(false).
                isMfaUser(false).
                isFederatedUser(true).
                federationProvider(federationProvider).
                build();

        return getLabUserData(query);
    }

    public static String getUserPassword(LabUser user){
        if(Strings.isNullOrEmpty(user.getCredentialUrl())){
            throw new IllegalArgumentException("Test setup: LabUser credential URL cannot be null");
        }
        if(keyVaultSecretsProvider == null){
            throw new IllegalArgumentException("Test setup: keyVaultSecretsProvider cannot be null");
        }
        try{
            return keyVaultSecretsProvider.getLabUserPassword(user.getCredentialUrl());
        } catch (Exception e){
            throw new InvalidStateException("Test setup: Cannot get the user password" + e.getMessage());
        }
    }
}
