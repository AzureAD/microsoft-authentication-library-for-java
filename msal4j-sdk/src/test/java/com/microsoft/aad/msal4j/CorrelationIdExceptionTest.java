// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify correlation IDs are included in exceptions
 */
class CorrelationIdExceptionTest {

    @Test
    void testMsalExceptionWithCorrelationId() {
        String testCorrelationId = "test-correlation-id-123";
        String message = "Test error message";
        String errorCode = "test_error";

        MsalException exception = new MsalException(message, errorCode, testCorrelationId);

        assertNotNull(exception.correlationId(), "Correlation ID should not be null");
        assertEquals(testCorrelationId, exception.correlationId(), "Correlation ID should match");
        assertTrue(exception.getMessage().contains(testCorrelationId), 
            "Exception message should contain correlation ID");
        assertTrue(exception.getMessage().contains("[Correlation ID: " + testCorrelationId + "]"),
            "Exception message should be formatted with correlation ID");
    }

    @Test
    void testMsalClientExceptionWithCorrelationId() {
        String testCorrelationId = "client-error-correlation-id";
        String message = "Client error occurred";
        String errorCode = "client_error";

        MsalClientException exception = new MsalClientException(message, errorCode, testCorrelationId);

        assertNotNull(exception.correlationId(), "Correlation ID should not be null");
        assertEquals(testCorrelationId, exception.correlationId(), "Correlation ID should match");
        assertTrue(exception.getMessage().contains(testCorrelationId),
            "Exception message should contain correlation ID");
    }

    @Test
    void testMsalServiceExceptionWithCorrelationId() {
        String testCorrelationId = "service-error-correlation-id";
        String message = "Service error occurred";
        String errorCode = "service_error";

        MsalServiceException exception = new MsalServiceException(message, errorCode, testCorrelationId);

        assertNotNull(exception.correlationId(), "Correlation ID should not be null");
        assertEquals(testCorrelationId, exception.correlationId(), "Correlation ID should match");
        assertTrue(exception.getMessage().contains(testCorrelationId),
            "Exception message should contain correlation ID");
    }

    @Test
    void testMsalThrottlingExceptionWithCorrelationId() {
        String testCorrelationId = "throttling-correlation-id";
        long retryInMs = 5000;

        MsalThrottlingException exception = new MsalThrottlingException(retryInMs, testCorrelationId);

        assertNotNull(exception.correlationId(), "Correlation ID should not be null");
        assertEquals(testCorrelationId, exception.correlationId(), "Correlation ID should match");
        assertTrue(exception.getMessage().contains(testCorrelationId),
            "Exception message should contain correlation ID");
        assertEquals(retryInMs, exception.retryInMs(), "Retry time should match");
    }

    @Test
    void testMsalExceptionWithoutCorrelationId() {
        String message = "Test error message";
        String errorCode = "test_error";

        MsalException exception = new MsalException(message, errorCode);

        assertNull(exception.correlationId(), "Correlation ID should be null when not provided");
        assertFalse(exception.getMessage().contains("[Correlation ID:"),
            "Exception message should not contain correlation ID prefix");
    }

    @Test
    void testLogHelperCreateMessage() {
        String message = "Test message";
        String correlationId = "test-corr-id";

        String formattedMessage = LogHelper.createMessage(message, correlationId);

        assertTrue(formattedMessage.contains("[Correlation ID: " + correlationId + "]"),
            "Formatted message should contain correlation ID in correct format");
        assertTrue(formattedMessage.contains(message),
            "Formatted message should contain original message");
    }
}
