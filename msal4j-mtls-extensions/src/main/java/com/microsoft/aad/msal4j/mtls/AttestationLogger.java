// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;
import org.slf4j.Logger;

final class AttestationLogger {

    private AttestationLogger() {
    }

    static AttestationLibrary.LogCallback create(
            Logger logger,
            String correlationId) {
        return (ctx, tag, level, function, line, message) -> {
            try {
                if (logger == null) {
                    return;
                }
                String prefix = "[MtlsPop][AttestationClientLib]"
                        + (correlationId == null || correlationId.isEmpty()
                        ? ""
                        : "[correlation_id=" + correlationId + "]");
                String location = text(tag) + " " + text(function) + ":" + line;

                switch (level) {
                    case 0:
                        logger.error("{} {} {}", prefix, location, text(message));
                        break;
                    case 1:
                        logger.warn("{} {} {}", prefix, location, text(message));
                        break;
                    case 2:
                        logger.info("{} {} {}", prefix, location, text(message));
                        break;
                    default:
                        // Native debug output can contain payload fragments. Do not emit
                        // the native message through ordinary non-PII application logs.
                        logger.debug("{} {} native debug event", prefix, location);
                        break;
                }
            } catch (RuntimeException ignored) {
                // A Java exception must never cross the native callback boundary.
            }
        };
    }

    private static String text(Pointer value) {
        if (value == null || Pointer.nativeValue(value) == 0) {
            return "";
        }
        try {
            String text = value.getString(0);
            return text == null ? "" : text;
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
