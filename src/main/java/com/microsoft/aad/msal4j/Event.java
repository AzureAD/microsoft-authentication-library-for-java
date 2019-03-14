package com.microsoft.aad.msal4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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

    private long startTimeStamp;

    Event(String eventName){
        this(eventName, new HashMap<>());
    }

    Event(String eventName, Map<String, String> predefined){
        super(predefined);

        this.put(EVENT_NAME_KEY, eventName);
        startTimeStamp = Instant.now().getEpochSecond();
        this.put(START_TIME_KEY, Long.toString(startTimeStamp));
        this.put(ELAPSED_TIME_KEY, "-1");
    }

    void stop(){
        long duration =  Instant.now().getEpochSecond() - startTimeStamp;
        this.put(ELAPSED_TIME_KEY, Long.toString(duration));
    }

    static String scrubTenant(URL url){

        URI uri;
        try {
            uri = url.toURI();
        } catch(URISyntaxException e){
            return null;
        }
        if(!uri.isAbsolute()){
            throw new IllegalArgumentException("Requires an absolute URI");
        }
        if(!Arrays.asList(AuthenticationAuthority.getTrustedHostList()).contains(uri.getHost())){
            return null;
        }

        //TODO should be updated when B2C is added, since tenant could be in different place
        String[] segment = uri.getPath().split("/");
        if(segment.length >= 2){
            segment[1] = TENANT_PLACEHOLDER;
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
