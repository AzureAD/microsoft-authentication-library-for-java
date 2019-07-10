// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

final class LogHelper {

    static String createMessage(String originalMessage, String correlationId) {
        return String.format("[Correlation ID: %s] " + originalMessage,
                correlationId);
    }

    static String getPiiScrubbedDetails(Throwable ex) {
        if (ex == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        sb.append(ex.getClass().getName());

        StackTraceElement[] stackTraceElements = ex.getStackTrace();
        for (StackTraceElement traceElement : stackTraceElements) {
            sb.append(System.getProperty("line.separator") + "\tat " + traceElement);
        }

        if (ex.getCause() != null) {
            sb.append(System.getProperty("line.separator") +
                    "Caused by: " + getPiiScrubbedDetails(ex.getCause()));
        }

        return sb.toString();
    }
}
