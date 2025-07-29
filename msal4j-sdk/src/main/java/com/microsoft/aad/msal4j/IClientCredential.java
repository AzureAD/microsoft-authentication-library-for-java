// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface representing an application credential used for client authentication.
 * <p>
 * Client credentials are used by confidential client applications to authenticate themselves
 * to the Microsoft identity platform when requesting tokens. This is used in scenarios where the
 * application is acting on its own behalf (client credentials flow) or on behalf of a user
 * (authorization code flow with client authentication).
 * <p>
 * MSAL supports several types of client credentials:
 * <ul>
 *   <li>Client secrets - A string secret configured for the application in the Azure portal</li>
 *   <li>Client certificates - An X.509 certificate that can be used to authenticate the application</li>
 *   <li>Client assertions - A JWT token signed with a private key that proves the client's identity</li>
 * </ul>
 * <p>
 * For more details, see https://aka.ms/msal4j-client-credentials
 */
public interface IClientCredential {

}
