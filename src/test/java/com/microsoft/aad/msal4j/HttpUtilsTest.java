// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test(groups = { "checkin" })
public class HttpUtilsTest {
    private final String COOKIE_HEADER_NAME = "Set-Cookie";
    private final String COOKIE_HEADER_VALUE_1 = "298zf09hf012fh2";
    private final String COOKIE_HEADER_VALUE_2 = "u32t4o3tb3gg43";

    @Test
    public void testHttpUtils_singleValueHeader() {
        Map<String, List<String>> singleValuedHeader = new HashMap<String, List<String>>() {{
            put(COOKIE_HEADER_NAME, Collections.singletonList(COOKIE_HEADER_VALUE_1));
        }};

        String headerValue = HttpUtils.headerValue(singleValuedHeader, COOKIE_HEADER_NAME);
        Assert.assertEquals(headerValue, COOKIE_HEADER_VALUE_1);
    }

    @Test
    public void testHttpUtils_multiValueHeader() {
        Map<String, List<String>> multiValuedHeader = new HashMap<String, List<String>>() {{
            put(COOKIE_HEADER_NAME, Arrays.asList(COOKIE_HEADER_VALUE_1, COOKIE_HEADER_VALUE_2));
        }};

        String headerValue = HttpUtils.headerValue(multiValuedHeader, COOKIE_HEADER_NAME);
        String expectedValue = COOKIE_HEADER_VALUE_1 + "," + COOKIE_HEADER_VALUE_2;
        Assert.assertEquals(headerValue, expectedValue);
    }

    @Test
    public void testHttpUtils_HeaderValueNull() {
        Map<String, List<String>> nullValuedHeader = new HashMap<String, List<String>>() {{
            put(COOKIE_HEADER_NAME, null);
        }};

        String headerValue = HttpUtils.headerValue(nullValuedHeader, COOKIE_HEADER_NAME);
        Assert.assertNull(headerValue);
    }
}
