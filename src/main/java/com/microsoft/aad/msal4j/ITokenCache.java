// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface representing security token cache persistence
 *
 * For more details, see https://aka.ms/msal4j-token-cache
 */
public interface ITokenCache {

    /**
     * Deserialize token cache from json
     *
     * @param data serialized cache in json format
     */
    void deserialize(String data);

    /**
     * Serialize token cache to json
     *
     * @return serialized cache in json format
     */
    String serialize();
}
