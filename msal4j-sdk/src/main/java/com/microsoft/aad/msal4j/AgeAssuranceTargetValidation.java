// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * An immutable, operation-local binding, not an authorization decision.
 * Access tokens and the age-assurance status claim are deliberately not inspected.
 */
final class AgeAssuranceTargetValidation {
    private static final String MESSAGE = "The ID token does not match the requested age-assurance targets.";
    private final String claims;
    private final Set<String> targets;

    private AgeAssuranceTargetValidation(String claims, Set<String> targets) {
        this.claims = claims;
        this.targets = Collections.unmodifiableSet(new HashSet<>(targets));
    }

    static AgeAssuranceTargetValidation fromParameters(IAcquireTokenParameters parameters) {
        if (parameters == null) {
            return fromClaims(null);
        }
        String claims = parameters.claims() == null ? null : parameters.claims().formatAsJSONString();
        if (parameters instanceof InteractiveRequestParameters) {
            claims = merge(claims, ((InteractiveRequestParameters) parameters).claimsChallenge());
        }
        claims = merge(claims, parameters.clientClaims());
        if (parameters.extraQueryParameters() != null && parameters.extraQueryParameters().containsKey("claims")) {
            claims = parameters.extraQueryParameters().get("claims");
        }
        return fromClaims(claims);
    }

    private static String merge(String claims, String additionalClaims) {
        if (StringHelper.isBlank(additionalClaims)) {
            return claims;
        }
        return StringHelper.isBlank(claims) ? additionalClaims : JsonHelper.mergeJSONString(claims, additionalClaims);
    }

    static AgeAssuranceTargetValidation fromClaims(String claims) {
        Set<String> targets = new HashSet<>();
        if (!StringHelper.isBlank(claims)) {
            Object idToken = member(readJson(claims), "id_token");
            Object properties = member(member(idToken, "xms_agvs"), "additionalProperties");
            if (properties instanceof List) {
                for (Object property : (List<?>) properties) {
                    if (property instanceof String
                            && ((String) property).regionMatches(true, 0, "target-", 0, "target-".length())) {
                        String text = (String) property;
                        targets.add(identity(text.substring("target-".length())));
                    }
                }
            }
        }
        return new AgeAssuranceTargetValidation(claims, targets);
    }

    boolean isRequired() {
        return !targets.isEmpty();
    }

    String claims() {
        return claims;
    }

    IAuthenticationResult validate(IAuthenticationResult result) {
        if (isRequired()) {
            validateIdToken(result == null ? null : result.idToken());
        }
        return result;
    }

    void validateIdToken(String idToken) {
        if (!isRequired()) {
            return;
        }
        try {
            if (idToken == null) {
                throw mismatch();
            }
            String[] parts = idToken.split("\\.", -1);
            if (parts.length != 3 || parts[1].isEmpty()) {
                throw mismatch();
            }
            Object receipt = member(readJson(new String(Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8)), "xms_agvt");
            if (!(receipt instanceof String)) {
                throw mismatch();
            }
            Set<String> received = new HashSet<>();
            for (String entry : ((String) receipt).split(",", -1)) {
                if (entry.length() < 2 || (entry.charAt(0) != '+' && entry.charAt(0) != '-')
                        || !received.add(identity(entry.substring(1)))) {
                    throw mismatch();
                }
            }
            if (!targets.equals(received)) {
                throw mismatch();
            }
        } catch (RuntimeException ex) {
            // Neither token contents nor parser exception details may escape this boundary.
            throw mismatch();
        }
    }

    private static String identity(String identity) {
        if (identity.isEmpty() || identity.charAt(0) == '+' || identity.charAt(0) == '-') {
            throw mismatch();
        }
        for (int i = 0; i < identity.length(); i++) {
            char character = identity.charAt(i);
            if (character == ',' || character == '\uFEFF'
                    || Character.isWhitespace(character) || Character.isSpaceChar(character)) {
                throw mismatch();
            }
        }
        return identity.toLowerCase(Locale.ROOT);
    }

    private static Object member(Object object, String name) {
        return object instanceof Map ? ((Map<?, ?>) object).get(name) : null;
    }

    private static Object readJson(String json) {
        try (JsonReader reader = JsonProviders.createReader(json)) {
            return reader.readUntyped();
        } catch (IOException | RuntimeException ex) {
            throw mismatch();
        }
    }

    private static MsalClientException mismatch() {
        return new MsalClientException(MESSAGE, AuthenticationErrorCode.AGE_ASSURANCE_TARGET_MISMATCH);
    }
}
