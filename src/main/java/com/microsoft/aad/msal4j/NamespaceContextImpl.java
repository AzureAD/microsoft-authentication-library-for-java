// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

class NamespaceContextImpl implements NamespaceContext {

    private final static Map<String, String> PREF_MAP = new HashMap<String, String>();

    static {
        PREF_MAP.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        PREF_MAP.put("sp",
                "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702");
        PREF_MAP.put("sp2005",
                "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy");
        PREF_MAP.put(
                "wsu",
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
        PREF_MAP.put("wsa10", "http://www.w3.org/2005/08/addressing");
        PREF_MAP.put("http",
                "http://schemas.microsoft.com/ws/06/2004/policy/http");
        PREF_MAP.put("soap12", "http://schemas.xmlsoap.org/wsdl/soap12/");
        PREF_MAP.put("wsp", "http://schemas.xmlsoap.org/ws/2004/09/policy");
        PREF_MAP.put("s", "http://www.w3.org/2003/05/soap-envelope");
        PREF_MAP.put("wsa", "http://www.w3.org/2005/08/addressing");
        PREF_MAP.put("wst", "http://docs.oasis-open.org/ws-sx/ws-trust/200512");
        PREF_MAP.put("t", "http://schemas.xmlsoap.org/ws/2005/02/trust");
        PREF_MAP.put("a", "http://www.w3.org/2005/08/addressing");
        PREF_MAP.put(
                "q",
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
    }

    public void modifyNameSpace(String key, String value) {
        PREF_MAP.put(key, value);
    }

    public String getNamespaceURI(String prefix) {
        return PREF_MAP.get(prefix);
    }

    public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }

}
