// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;

class HttpUtils {

    static String headerValue(Map<String, List<String>> headers, String headerName){

        if(headerName == null || headers == null){
            return null;
        }

        List<String> headerValue = headers.get(headerName);

        if(headerValue == null || headerValue.isEmpty()){
            return null;
        }

        return headerValue.get(0);
    }
}
