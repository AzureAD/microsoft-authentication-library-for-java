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

package com.microsoft.aad.msal4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

abstract class Event extends HashMap<String, String>{

    final static String EVENT_NAME_KEY = "event_name";
    final static String START_TIME_KEY = "start_time";
    final static String ELAPSED_TIME_KEY = "elapsed_time";
    private final static String TENANT_PLACEHOLDER = "<tenant>";
    private final static String USERNAME_PLACEHOLDER = "<user>";

    private long startTimeStamp;

    Event(String eventName){
        this(eventName, new HashMap<>());
    }

    Event(String eventName, Map<String, String> predefined){
        super(predefined);

        this.put(EVENT_NAME_KEY, eventName);
        startTimeStamp = Instant.now().toEpochMilli();
        this.put(START_TIME_KEY, Long.toString(startTimeStamp));
        this.put(ELAPSED_TIME_KEY, "-1");
    }

    void stop(){
        long duration =  Instant.now().toEpochMilli() - startTimeStamp;
        this.put(ELAPSED_TIME_KEY, Long.toString(duration));
    }

    static String scrubTenant(URI uri){
        if(!uri.isAbsolute()){
            throw new IllegalArgumentException("Requires an absolute URI");
        }
        if(!AadInstanceDiscovery.TRUSTED_HOSTS_SET.contains(uri.getHost())){
            return null;
        }

        String[] segment = uri.getPath().split("/");
        if(segment.length >= 2){
            if(segment[1].equals("tfp")){
                segment[2] = TENANT_PLACEHOLDER;
            } else {
                segment[1] = TENANT_PLACEHOLDER;
            }

            if(segment.length >= 3 && segment[2].equals("userrealm")){
                segment[3] = USERNAME_PLACEHOLDER;
            }
        }

        String scrubbedPath = String.join("/", segment);
        return uri.getScheme() + "://" + uri.getAuthority() + scrubbedPath;
    }

    static String hashPii(String stringToHash){
        String base64EncodedSha256Hash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedString = digest.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
             base64EncodedSha256Hash = Base64.getEncoder().encode(hashedString).toString();
        } catch(NoSuchAlgorithmException e){
            base64EncodedSha256Hash = null;
        }
        return base64EncodedSha256Hash;
    }
}
