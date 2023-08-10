// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpUtilsTest {

    private final String COOKIE_HEADER_NAME = "Set-Cookie";
    private final String COOKIE_HEADER_VALUE_1 = "298zf09hf012fh2";
    private final String COOKIE_HEADER_VALUE_2 = "u32t4o3tb3gg43";

    @Test
    void testHttpUtils_singleValueHeader() {

        Map<String, List<String>> singleValuedHeader = new HashMap<String, List<String>>() {{
            put(COOKIE_HEADER_NAME, Collections.singletonList(COOKIE_HEADER_VALUE_1));
        }};

        String headerValue = HttpUtils.headerValue(singleValuedHeader, COOKIE_HEADER_NAME);
        assertEquals(headerValue, COOKIE_HEADER_VALUE_1);
    }

    @Test
    void testHttpUtils_multiValueHeader() {

        Map<String, List<String>> multiValuedHeader = new HashMap<String, List<String>>() {{
            put(COOKIE_HEADER_NAME, Arrays.asList(COOKIE_HEADER_VALUE_1, COOKIE_HEADER_VALUE_2));
        }};

        String headerValue = HttpUtils.headerValue(multiValuedHeader, COOKIE_HEADER_NAME);
        String expectedValue = COOKIE_HEADER_VALUE_1 + "," + COOKIE_HEADER_VALUE_2;
        assertEquals(headerValue, expectedValue);
    }

    @Test
    void testHttpUtils_HeaderValueNull() {

        Map<String, List<String>> nullValuedHeader = new HashMap<String, List<String>>() {{
            put(COOKIE_HEADER_NAME, null);
        }};

        String headerValue = HttpUtils.headerValue(nullValuedHeader, COOKIE_HEADER_NAME);
        assertNull(headerValue);
    }
}
