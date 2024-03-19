// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/** An extension for the default HttpClient which is meant to perform any extra HTTP behavior needed for a managed identity flow.
 * <p>
 * Currently the only extra behavior is the Service Fabric flow, where we must add a certificate thumbprint to the HTTP connection.
 */
class DefaultHttpClientManagedIdentity extends DefaultHttpClient {

    public static final HostnameVerifier ALL_HOSTS_ACCEPT_HOSTNAME_VERIFIER;

    static {
        ALL_HOSTS_ACCEPT_HOSTNAME_VERIFIER = new HostnameVerifier() {
            @SuppressWarnings("BadHostnameVerifier")
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    }

    DefaultHttpClientManagedIdentity(Proxy proxy, SSLSocketFactory sslSocketFactory, Integer connectTimeout, Integer readTimeout) {
        super(proxy, sslSocketFactory, connectTimeout, readTimeout);
    }

    @Override
    HttpURLConnection openConnection(final URL finalURL)
            throws IOException {
        URLConnection connection;

        if (proxy != null) {
            connection = finalURL.openConnection(proxy);
        } else {
            connection = finalURL.openConnection();
        }

        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        if (connection instanceof HttpURLConnection) {
            return (HttpURLConnection) connection;
        } else {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

            if (sslSocketFactory != null) {
                httpsConnection.setSSLSocketFactory(sslSocketFactory);
            }

            if (System.getenv(Constants.IDENTITY_SERVER_THUMBPRINT) != null) {
                addTrustedCertificateThumbprint(httpsConnection, System.getenv(Constants.IDENTITY_SERVER_THUMBPRINT));
            }

            return httpsConnection;
        }
    }

    /**
     *
     * Pins the specified HTTPS URL Connection to work against a specific server-side certificate with
     * the specified thumbprint only.
     *
     * @param httpsUrlConnection The https url connection to configure
     * @param certificateThumbprint The thumbprint of the certificate
     */
    public static void addTrustedCertificateThumbprint(HttpsURLConnection httpsUrlConnection,
                                                       String certificateThumbprint) {
        //We expect the connection to work against a specific server side certificate only, so it's safe to disable the
        // host name verification.
        if (httpsUrlConnection.getHostnameVerifier() != ALL_HOSTS_ACCEPT_HOSTNAME_VERIFIER) {
            httpsUrlConnection.setHostnameVerifier(ALL_HOSTS_ACCEPT_HOSTNAME_VERIFIER);
        }

        // Create a Trust manager that trusts only certificate with specified thumbprint.
        TrustManager[] certificateTrust = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            public void checkClientTrusted(X509Certificate[] certificates, String authenticationType)
                    throws CertificateException {
                throw new CertificateException("No client side certificate configured.");
            }

            public void checkServerTrusted(X509Certificate[] certificates, String authenticationType)
                    throws CertificateException {
                if (certificates == null || certificates.length == 0) {
                    throw new CertificateException("Did not receive any certificate from the server.");
                }

                for (X509Certificate x509Certificate : certificates) {
                    String sslCertificateThumbprint = extractCertificateThumbprint(x509Certificate);
                    if (certificateThumbprint.equalsIgnoreCase(sslCertificateThumbprint)) {
                        return;
                    }
                }
                throw new RuntimeException("Thumbprint of certificates received did not match the expected thumbprint.");
            }
        }
        };

        SSLSocketFactory sslSocketFactory;
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, certificateTrust, null);
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Error Creating SSL Context", e);
        }

        // Pin the connection to a specific certificate with specified thumbprint.
        if (httpsUrlConnection.getSSLSocketFactory() != sslSocketFactory) {
            httpsUrlConnection.setSSLSocketFactory(sslSocketFactory);
        }
    }

    private static String extractCertificateThumbprint(Certificate certificate) {
        try {
            StringBuilder thumbprint = new StringBuilder();
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

            byte[] encodedCertificate;

            try {
                encodedCertificate = certificate.getEncoded();
            } catch (CertificateEncodingException e) {
                throw new RuntimeException(e);
            }

            byte[] updatedDigest = messageDigest.digest(encodedCertificate);

            for (byte b : updatedDigest) {
                int unsignedByte = b & 0xff;

                if (unsignedByte < 16) {
                    thumbprint.append("0");
                }
                thumbprint.append(Integer.toHexString(unsignedByte));
            }
            return thumbprint.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new MsalClientException("NoSuchAlgorithmException when extracting certificate thumbprint: ", e.getMessage());
        }
    }

}
