// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

class IllegalArgumentExceptionMessages {

    final static String AUTHORITY_URI_EMPTY_PATH_SEGMENT = "Authority Uri should not have empty path segments";

    final static String AUTHORITY_URI_MISSING_PATH_SEGMENT = "Authority Uri must have at least one path segment. This is usually 'common' or the application's tenant id.";

    final static String AUTHORITY_URI_EMPTY_PATH = "Authority Uri should have at least one segment in the path";

}
