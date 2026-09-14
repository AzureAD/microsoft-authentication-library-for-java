// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Memory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AttestationLoggerTest {

    @Test
    void mapsNativeLevelsAndIncludesCorrelationId() {
        Logger logger = mock(Logger.class);
        AttestationLibrary.LogCallback callback =
                AttestationLogger.create(logger, "correlation-id");

        callback.log(null, text("tag"), 0, text("function"), 42, text("error"));
        callback.log(null, text("tag"), 1, text("function"), 42, text("warning"));
        callback.log(null, text("tag"), 2, text("function"), 42, text("info"));
        callback.log(null, text("tag"), 3, text("function"), 42, text("secret"));

        verify(logger).error(
                eq("{} {} {}"),
                any(),
                any(),
                eq("error"));
        verify(logger).warn(
                eq("{} {} {}"),
                any(),
                any(),
                eq("warning"));
        verify(logger).info(
                eq("{} {} {}"),
                any(),
                any(),
                eq("info"));
        verify(logger).debug(
                eq("{} {} native debug event"),
                any(),
                any());
    }

    @Test
    void loggerFailureNeverEscapesNativeCallback() {
        Logger logger = mock(Logger.class);
        doThrow(new IllegalStateException("logger failure"))
                .when(logger)
                .error(any(String.class), any(), any(), any());

        AttestationLibrary.LogCallback callback =
                AttestationLogger.create(logger, null);

        assertDoesNotThrow(() -> callback.log(
                null,
                text("tag"),
                0,
                text("function"),
                1,
                text("message")));
    }

    private static Memory text(String value) {
        Memory memory = new Memory(value.length() + 1L);
        memory.setString(0, value);
        return memory;
    }
}
