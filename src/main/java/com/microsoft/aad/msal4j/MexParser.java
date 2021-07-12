// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class MexParser {

    private final static Logger log = LoggerFactory.getLogger(MexParser.class);

    private final static String TRANSPORT_BINDING_XPATH = "wsp:ExactlyOne/wsp:All/sp:TransportBinding";
    private final static String TRANSPORT_BINDING_2005_XPATH = "wsp:ExactlyOne/wsp:All/sp2005:TransportBinding";
    private final static String PORT_XPATH = "//wsdl:definitions/wsdl:service/wsdl:port";
    private final static String ADDRESS_XPATH = "wsa10:EndpointReference/wsa10:Address";
    private final static String SOAP_ACTION_XPATH = "wsdl:operation/soap12:operation/@soapAction";
    private final static String RST_SOAP_ACTION = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue";
    private final static String RST_SOAP_ACTION_2005 = "http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue";
    private final static String SOAP_TRANSPORT_XPATH = "soap12:binding/@transport";
    private final static String SOAP_HTTP_TRANSPORT_VALUE = "http://schemas.xmlsoap.org/soap/http";

    private interface PolicySelector {
        Map<String, BindingPolicy> selectPolicies(Document document, XPath xPath, boolean logPii) throws XPathExpressionException;
    }

    private static class NegotiateAuthenticationPolicySelector implements PolicySelector {
        public Map<String, BindingPolicy> selectPolicies(Document xmlDocument, XPath xPath, boolean logPii) throws XPathExpressionException {
            String xpathExpression = "//wsdl:definitions/wsp:Policy/wsp:ExactlyOne/wsp:All/http:NegotiateAuthentication";

            return selectIntegratedPoliciesWithExpression(xmlDocument, xPath, xpathExpression);
        }
    }

    private static class WsTrustEndpointPolicySelector implements PolicySelector {
        public Map<String, BindingPolicy> selectPolicies(Document xmlDocument, XPath xPath, boolean logPii)
                throws XPathExpressionException {
            String xpathExpression = "//wsdl:definitions/wsp:Policy/wsp:ExactlyOne/wsp:All/"
                    + "sp:SignedEncryptedSupportingTokens/wsp:Policy/sp:UsernameToken/"
                    + "wsp:Policy/sp:WssUsernameToken10";
            Map<String, BindingPolicy> policies = selectUsernamePasswordPoliciesWithExpression(
                    xmlDocument, xPath, xpathExpression, logPii);

            ((NamespaceContextImpl) xPath.getNamespaceContext()).modifyNameSpace("sp",
                    "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy");

            xpathExpression = "//wsdl:definitions/wsp:Policy/wsp:ExactlyOne/wsp:All/"
                    + "sp:SignedSupportingTokens/wsp:Policy/sp:UsernameToken/"
                    + "wsp:Policy/sp:WssUsernameToken10";
            policies.putAll(selectUsernamePasswordPoliciesWithExpression(
                    xmlDocument, xPath, xpathExpression, logPii));

            return policies;
        }
    }

    static BindingPolicy getPolicy(String mexResponse, PolicySelector policySelector, boolean logPii)
            throws Exception {
        DocumentBuilderFactory builderFactory = SafeDocumentBuilderFactory.createInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        Document xmlDocument = builder.parse(new ByteArrayInputStream(
                mexResponse.getBytes(StandardCharsets.UTF_8)));

        XPath xPath = XPathFactory.newInstance().newXPath();
        NamespaceContextImpl nameSpace = new NamespaceContextImpl();
        xPath.setNamespaceContext(nameSpace);

        Map<String, BindingPolicy> policies = policySelector.selectPolicies(xmlDocument, xPath, logPii);

        if (policies.isEmpty()) {
            log.debug("No matching policies");

            return null;
        } else {
            Map<String, BindingPolicy> bindings = getMatchingBindings(
                    xmlDocument, xPath, policies, logPii);

            if (bindings.isEmpty()) {
                log.debug("No matching bindings");

                return null;
            } else {
                getPortsForPolicyBindings(xmlDocument, xPath, bindings, policies, logPii);
                return selectSingleMatchingPolicy(policies);
            }
        }
    }

    static BindingPolicy getPolicyFromMexResponseForIntegrated(String mexResponse, boolean logPii) throws Exception {
        return getPolicy(mexResponse, new NegotiateAuthenticationPolicySelector(), logPii);
    }

    static BindingPolicy getWsTrustEndpointFromMexResponse(String mexResponse, boolean logPii)
            throws Exception {
        return getPolicy(mexResponse, new WsTrustEndpointPolicySelector(), logPii);
    }

    private static BindingPolicy selectSingleMatchingPolicy(
            Map<String, BindingPolicy> policies) {

        BindingPolicy wstrust13 = null, wstrust2005 = null;

        // Select wstrust13 first if wstrust13 available
        Iterator<Entry<String, BindingPolicy>> it = policies.entrySet()
                .iterator();
        while (it.hasNext()) {
            Map.Entry<String, BindingPolicy> pair = it.next();
            if (pair.getValue().getUrl() != null) {
                if (pair.getValue().getVersion() == WSTrustVersion.WSTRUST13) {
                    wstrust13 = pair.getValue();
                } else if (pair.getValue().getVersion() == WSTrustVersion.WSTRUST2005) {
                    wstrust2005 = pair.getValue();
                }
            }
        }

        if (wstrust13 == null && wstrust2005 == null) {
            log.warn("No policies found with the url");

            return null;
        }

        return wstrust13 != null ? wstrust13 : wstrust2005;
    }

    private static void getPortsForPolicyBindings(Document xmlDocument,
                                                  XPath xPath, Map<String, BindingPolicy> bindings,
                                                  Map<String, BindingPolicy> policies, boolean logPii) throws Exception {

        NodeList portNodes = (NodeList) xPath.compile(PORT_XPATH).evaluate(
                xmlDocument, XPathConstants.NODESET);

        if (portNodes.getLength() == 0) {
            log.warn("No ports found");
        } else {
            for (int i = 0; i < portNodes.getLength(); i++) {
                Node portNode = portNodes.item(i);
                String bindingId = portNode.getAttributes()
                        .getNamedItem("binding").getNodeValue();
                String[] bindingIdParts = bindingId.split(":");
                bindingId = bindingIdParts[bindingIdParts.length - 1];
                BindingPolicy trustPolicy = bindings.get(bindingId);
                if (trustPolicy != null) {
                    BindingPolicy bindingPolicy = policies.get(trustPolicy
                            .getUrl());
                    if (bindingPolicy != null
                            && StringHelper.isBlank(bindingPolicy.getUrl())) {
                        bindingPolicy.setVersion(trustPolicy.getVersion());
                        NodeList addressNodes = (NodeList) xPath.compile(
                                ADDRESS_XPATH).evaluate(portNode,
                                XPathConstants.NODESET);
                        if (addressNodes.getLength() > 0) {
                            String address = addressNodes.item(0)
                                    .getTextContent();
                            if (address != null
                                    && address.toLowerCase().startsWith(
                                    "https://")) {
                                bindingPolicy.setUrl(address.trim());
                            } else {
                                if (logPii) {
                                    log.warn("Skipping insecure endpoint" + ": " + address);
                                } else {
                                    log.warn("Skipping insecure endpoint");
                                }
                            }
                        } else {
                            throw new MsalClientException(
                                    "Error parsing WSTrustResponse: No address nodes on port",
                                    AuthenticationErrorCode.WSTRUST_INVALID_RESPONSE);
                        }
                    }
                }
            }
        }
    }

    private static Map<String, BindingPolicy> getMatchingBindings(
            Document xmlDocument, XPath xPath,
            Map<String, BindingPolicy> policies, boolean logPii)
            throws XPathExpressionException {
        Map<String, BindingPolicy> bindings = new HashMap<String, BindingPolicy>();
        NodeList nodeList = (NodeList) xPath.compile(
                "//wsdl:definitions/wsdl:binding/wsp:PolicyReference")
                .evaluate(xmlDocument, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            String uri = node.getAttributes().getNamedItem("URI")
                    .getNodeValue();
            if (policies.containsKey(uri)) {
                Node bindingNode = node.getParentNode();
                String bindingName = bindingNode.getAttributes()
                        .getNamedItem("name").getNodeValue();

                WSTrustVersion version = checkSoapActionAndTransport(xPath,
                        bindingNode, logPii);
                if (version != WSTrustVersion.UNDEFINED) {
                    BindingPolicy policy = new BindingPolicy("");
                    policy.setUrl(uri);
                    policy.setVersion(version);
                    bindings.put(bindingName, policy);
                }
            }
        }
        return bindings;
    }

    private static WSTrustVersion checkSoapActionAndTransport(XPath xPath,
                                                              Node bindingNode, boolean logPii) throws XPathExpressionException {
        NodeList soapTransportAttributes = null;
        String soapAction = null;
        String bindingName = bindingNode.getAttributes().getNamedItem("name")
                .getNodeValue();
        NodeList soapActionAttributes = (NodeList) xPath.compile(
                SOAP_ACTION_XPATH)
                .evaluate(bindingNode, XPathConstants.NODESET);
        if (soapActionAttributes.getLength() > 0) {
            soapAction = soapActionAttributes.item(0).getNodeValue();
            soapTransportAttributes = (NodeList) xPath.compile(
                    SOAP_TRANSPORT_XPATH).evaluate(bindingNode,
                    XPathConstants.NODESET);
            if (soapTransportAttributes != null
                    && soapTransportAttributes.getLength() > 0
                    && soapTransportAttributes.item(0).getNodeValue()
                    .equalsIgnoreCase(SOAP_HTTP_TRANSPORT_VALUE)) {

                if (soapAction.equalsIgnoreCase(RST_SOAP_ACTION)) {
                    if (logPii) {
                        log.debug("Found binding matching Action and Transport: " + bindingName);
                    } else {
                        log.debug("Found binding matching Action and Transport");
                    }

                    return WSTrustVersion.WSTRUST13;
                } else if (soapAction.equalsIgnoreCase(RST_SOAP_ACTION_2005)) {
                    if (logPii) {
                        log.debug("Binding node did not match soap Action or Transport: " + bindingName);
                    } else {
                        log.debug("Binding node did not match soap Action or Transport");
                    }

                    return WSTrustVersion.WSTRUST2005;
                }
            }
        }

        return WSTrustVersion.UNDEFINED;
    }

    private static Map<String, BindingPolicy> selectUsernamePasswordPoliciesWithExpression(
            Document xmlDocument, XPath xPath, String xpathExpression, boolean logPii)
            throws XPathExpressionException {

        Map<String, BindingPolicy> policies = new HashMap<String, BindingPolicy>();

        NodeList nodeList = (NodeList) xPath.compile(xpathExpression).evaluate(
                xmlDocument, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {

            // Nodes
            // sp:WssUsernameToken10
            // wsp:Policy
            // sp:UsernameToken
            // wsp:Policy
            // sp:SignedEncryptedSupportingTokens
            // wsp:All
            // wsp:ExactlyOne
            // wsp:Policy
            String policy = checkPolicy(xPath, nodeList.item(i).getParentNode()
                    .getParentNode().getParentNode().getParentNode()
                    .getParentNode().getParentNode().getParentNode(), logPii);
            policies.put("#" + policy, new BindingPolicy("#" + policy));
        }
        return policies;
    }

    private static Map<String, BindingPolicy> selectIntegratedPoliciesWithExpression(Document xmlDocument,
                                                                                     XPath xPath,
                                                                                     String xpathExpression) throws XPathExpressionException {

        Map<String, BindingPolicy> policies = new HashMap<String, BindingPolicy>();

        NodeList nodeList = (NodeList) xPath.compile(xpathExpression).evaluate(xmlDocument, XPathConstants.NODESET);
        for (int i = 0; i < nodeList.getLength(); i++) {
            // get back to //wsdl:definitions/wsp:Policy
            String policy = checkPolicyIntegrated(xPath, nodeList.item(i).getParentNode().getParentNode().getParentNode());
            policies.put("#" + policy, new BindingPolicy("#" + policy));
        }
        return policies;
    }

    private static String checkPolicy(XPath xPath, Node node, boolean logPii)
            throws XPathExpressionException {

        String policyId = null;
        Node id = node.getAttributes().getNamedItem("wsu:Id");
        NodeList transportBindingNodes = (NodeList) xPath.compile(
                TRANSPORT_BINDING_XPATH).evaluate(node, XPathConstants.NODESET);
        if (transportBindingNodes.getLength() == 0) {
            transportBindingNodes = (NodeList) xPath.compile(
                    TRANSPORT_BINDING_2005_XPATH).evaluate(node,
                    XPathConstants.NODESET);
        }

        if (transportBindingNodes.getLength() > 0 && id != null) {
            policyId = id.getNodeValue();

            if (logPii) {
                log.debug("found matching policy id: " + policyId);
            } else {
                log.debug("found matching policy");
            }
        } else {
            String nodeValue = "none";
            if (id != null) {
                nodeValue = id.getNodeValue();
            }

            if (logPii) {
                log.debug("potential policy did not match required transport binding: " + nodeValue);
            } else {
                log.debug("potential policy did not match required transport binding");
            }
        }
        return policyId;
    }

    private static String checkPolicyIntegrated(XPath xPath,
                                                Node node) throws XPathExpressionException {
        Node id = node.getAttributes().getNamedItem("wsu:Id");
        String policyId = id.getNodeValue();
        return policyId;
    }
}
