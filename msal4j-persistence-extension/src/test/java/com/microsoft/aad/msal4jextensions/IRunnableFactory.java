// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

public interface IRunnableFactory {
    Runnable create(String id);
}
