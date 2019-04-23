// sample was written based on https://docs.oracle.com/javase/7/docs/technotes/guides/security/jsse/samples/sockets/client/SSLSocketClientWithTunneling.java
// here is the copyright notice:

/*
 *
 * Copyright (c) 1994, 2004, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * -Redistribution of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * Redistribution in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Oracle nor the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT
 * OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */


import com.nimbusds.jose.util.Base64;

import java.net.*;
import java.io.*;
import javax.net.ssl.*;


/**
 * SSLSocketFactory for tunneling ssl sockets through a proxy with Basic Authorization
 */
public class SSLTunnelSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory defaultFactory;

    private String tunnelHost;

    private int tunnelPort;

    private String proxyUserName;

    private String proxyPassword;

    public SSLTunnelSocketFactory(String proxyHost, String proxyPort,
                                  String proxyUserName, String proxyPassword) {
        tunnelHost = proxyHost;
        tunnelPort = Integer.parseInt(proxyPort);
        defaultFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        this.proxyUserName = proxyUserName;
        this.proxyPassword = proxyPassword;
    }

    public Socket createSocket(String host, int port) throws IOException {
        return createSocket(null, host, port, true);
    }

    public Socket createSocket(String host, int port, InetAddress clientHost,
                               int clientPort) throws IOException {
        return createSocket(null, host, port, true);
    }

    public Socket createSocket(InetAddress host, int port) throws IOException {
        return createSocket(null, host.getHostName(), port, true);
    }

    public Socket createSocket(InetAddress address, int port,
                               InetAddress clientAddress, int clientPort) throws IOException {
        return createSocket(null, address.getHostName(), port, true);
    }

    public Socket createSocket(Socket s, String host, int port,
                               boolean autoClose) throws IOException {

        Socket tunnel = new Socket(tunnelHost, tunnelPort);

        doTunnelHandshake(tunnel, host, port);

        SSLSocket result = (SSLSocket) defaultFactory.createSocket(tunnel, host,
                port, autoClose);

        return result;
    }

    private void doTunnelHandshake(Socket tunnel, String host, int port)
            throws IOException {
        OutputStream out = tunnel.getOutputStream();

        String token = proxyUserName + ":" + proxyPassword;
        String authString = "Basic " + Base64.encode(token.getBytes());

        String msg = "CONNECT " + host + ":" + port + " HTTP/1.1\n"
                + "User-Agent: " + sun.net.www.protocol.http.HttpURLConnection.userAgent + "\n"
                + "Proxy-Authorization: " + authString
                + "\r\n\r\n";

        out.write(msg.getBytes("UTF-8"));
        out.flush();


        StringBuilder replyStr = new StringBuilder();
        int newlinesSeen = 0;
        boolean headerDone = false; /* Done on first newline */

        InputStream in = tunnel.getInputStream();

        while (newlinesSeen < 2) {
            int i = in.read();
            if (i < 0) {
                throw new IOException("Unexpected EOF from proxy");
            }
            if (i == '\n') {
                headerDone = true;
                ++newlinesSeen;
            } else if (i != '\r') {
                newlinesSeen = 0;
                if (!headerDone) {
                    replyStr.append((char) i);
                }
            }
        }

        if (replyStr.toString().toLowerCase().indexOf("200 connection established") == -1) {
            throw new IOException("Unable to tunnel through " + tunnelHost
                    + ":" + tunnelPort + ".  Proxy returns \"" + replyStr + "\"");
        }
    }

    public String[] getDefaultCipherSuites() {
        return defaultFactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return defaultFactory.getSupportedCipherSuites();
    }
}