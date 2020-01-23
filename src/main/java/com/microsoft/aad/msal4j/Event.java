// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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
        if(!AadInstanceDiscoveryProvider.TRUSTED_HOSTS_SET.contains(uri.getHost())){
            return null;
        }

        String[] segment = uri.getPath().split("/");

        if(segment.length >= 2){
            if(segment[1].equals("tfp") && segment.length >= 3){
                segment[2] = TENANT_PLACEHOLDER;
            } else {
                segment[1] = TENANT_PLACEHOLDER;
            }
            if(segment.length >= 4 && segment[2].equals("userrealm")){
                segment[3] = USERNAME_PLACEHOLDER;
            }
        }

        String scrubbedPath = String.join("/", segment);
        return uri.getScheme() + "://" + uri.getAuthority() + scrubbedPath;
    }
}
