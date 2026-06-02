// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeTest {

    @Test
    void parseUnixTimestampInSeconds() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long result = AcquireTokenByManagedIdentitySupplier.getExpiresOnFromManagedIdentityTimestamp(String.valueOf(currentTimestamp));

        assertEquals(currentTimestamp, result, "Should parse Unix timestamp in seconds correctly");
    }

    @Test
    void parseIso8601Format() {
        // Creates a timestamp in ISO 8601 format, with 24 hours added to it to represent a 24-hour token
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plus(24, ChronoUnit.HOURS));

        long expected = Instant.parse(timestamp).getEpochSecond();
        long result = AcquireTokenByManagedIdentitySupplier.getExpiresOnFromManagedIdentityTimestamp(timestamp);

        assertEquals(expected, result, "Should parse ISO 8601 format correctly");
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "2025-05-15T12:34:56Z",              // Basic UTC format
            "2025-05-15T12:34:56.1234Z",          // With milliseconds
            "2025-05-15T12:34:56.123456789Z"     // With nanoseconds
    })
    void testValidIso8601Formats(String timestamp) {
        long expected = Instant.parse(timestamp).getEpochSecond();
        long result = AcquireTokenByManagedIdentitySupplier.getExpiresOnFromManagedIdentityTimestamp(timestamp);

        assertEquals(expected, result, "Should parse ISO 8601 format correctly");
    }

    @Test
    void handleNullTimestamp() {
        long result = AcquireTokenByManagedIdentitySupplier.getExpiresOnFromManagedIdentityTimestamp(null);

        assertEquals(0, result, "Should return 0 for null timestamp");
    }

    @Test
    void handleEmptyTimestamp() {
        long result = AcquireTokenByManagedIdentitySupplier.getExpiresOnFromManagedIdentityTimestamp("");

        assertEquals(0, result, "Should return 0 for empty timestamp");
    }

    @Test
    void handleInvalidFormat() {
        String invalidTimestamp = "not-a-timestamp";

        MsalClientException exception = assertThrows(
                MsalClientException.class,
                () -> AcquireTokenByManagedIdentitySupplier.getExpiresOnFromManagedIdentityTimestamp(invalidTimestamp)
        );

        assertEquals("invalid_timestamp_format", exception.errorCode());
        assertTrue(exception.getMessage().contains(invalidTimestamp),
                "Error message should contain the invalid timestamp");
    }
}
