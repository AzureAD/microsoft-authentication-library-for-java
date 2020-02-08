// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

public enum ResponseMode {
    FORM_POST("form_post"),
    QUERY("query"),
    FRAGMENT("fragment");

    private String responseMode;

    ResponseMode(String responseMode){
        this.responseMode = responseMode;
    }

    @Override
    public String toString(){
        return this.responseMode;
    }
}
