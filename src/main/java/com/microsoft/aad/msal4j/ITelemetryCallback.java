package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;

public interface ITelemetryCallback {
    void onTelemetryCallback(List<? extends Map<String, String>> events);
}
