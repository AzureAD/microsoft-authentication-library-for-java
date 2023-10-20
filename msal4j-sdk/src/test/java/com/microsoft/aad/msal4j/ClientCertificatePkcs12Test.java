// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyStore;
import java.security.KeyStoreSpi;
import java.util.Arrays;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientCertificatePkcs12Test {

    private KeyStoreSpi keyStoreSpi;
    private KeyStore keystore;

    @BeforeEach
    void setUp() throws Exception {
        keyStoreSpi = mock(KeyStoreSpi.class);
        keystore = new KeyStore(keyStoreSpi, null, "PKCS12") {};
        keystore.load(null);
    }

    @Test
    void testNoEntries() {
        doReturn(Collections.enumeration(Collections.emptyList())).when(keyStoreSpi).engineAliases();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClientCertificate.getPrivateKeyAlias(keystore));

        assertTrue(ex.getMessage().contains("certificate not loaded from input stream"));
    }

    @Test
    void testNoPrivateKey() {
        doReturn(Collections.enumeration(Arrays.asList("CA_cert1", "CA_cert2"))).when(keyStoreSpi).engineAliases();
        doReturn(false).when(keyStoreSpi).engineEntryInstanceOf("CA_cert1", KeyStore.PrivateKeyEntry.class);
        doReturn(false).when(keyStoreSpi).engineEntryInstanceOf("CA_cert2", KeyStore.PrivateKeyEntry.class);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClientCertificate.getPrivateKeyAlias(keystore));

        assertTrue(ex.getMessage().contains("certificate not loaded from input stream"));
    }

    @Test
    void testMultiplePrivateKeyAliases() {
        doReturn(Collections.enumeration(Arrays.asList("private_key1", "private_key2", "CA_cert"))).when(keyStoreSpi).engineAliases();
        doReturn(true).when(keyStoreSpi).engineEntryInstanceOf("private_key1", KeyStore.PrivateKeyEntry.class);
        doReturn(true).when(keyStoreSpi).engineEntryInstanceOf("private_key2", KeyStore.PrivateKeyEntry.class);
        lenient().doReturn(false).when(keyStoreSpi).engineEntryInstanceOf("CA_cert", KeyStore.PrivateKeyEntry.class);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClientCertificate.getPrivateKeyAlias(keystore));

        assertTrue(ex.getMessage().contains("more than one certificate alias found in input stream"));
    }

    @Test
    void testMultipleEntriesButOnlyOnePrivateKey() throws Exception {

        doReturn(Collections.enumeration(Arrays.asList("CA_cert1", "private_key", "CA_cert2"))).when(keyStoreSpi).engineAliases();
        doReturn(false).when(keyStoreSpi).engineEntryInstanceOf("CA_cert1", KeyStore.PrivateKeyEntry.class);
        doReturn(true).when(keyStoreSpi).engineEntryInstanceOf("private_key", KeyStore.PrivateKeyEntry.class);
        doReturn(false).when(keyStoreSpi).engineEntryInstanceOf("CA_cert2", KeyStore.PrivateKeyEntry.class);

        String privateKeyAlias = ClientCertificate.getPrivateKeyAlias(keystore);
        assertEquals("private_key", privateKeyAlias);
    }

}