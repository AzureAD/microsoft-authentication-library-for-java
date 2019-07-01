// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.Reader;
import java.io.StringReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 */
class JsonHelper {
    /**
     * 
     * @param json
     * @param clazz
     * @return
     */
    static <T> T convertJsonToObject(final String json, final Class<T> clazz) {
        final Reader reader = new StringReader(json);
        final Gson gson = new GsonBuilder().create();
        return gson.fromJson(reader, clazz);
    }
}
