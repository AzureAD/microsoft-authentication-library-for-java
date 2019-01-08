// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public class NamespaceContextImpl implements NamespaceContext {

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
