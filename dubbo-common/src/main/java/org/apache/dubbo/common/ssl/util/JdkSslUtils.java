/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.common.ssl.util;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE_STREAM;

public class JdkSslUtils {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(JdkSslUtils.class);

    public static SSLContext buildJdkSSLContext(InputStream keyCertChainPathStream,
                                                InputStream privateKeyPathStream,
                                                InputStream trustCertStream, String password) {
        return buildJdkSSLContext(keyCertChainPathStream, privateKeyPathStream, trustCertStream, strPasswordToCharArray(password));
    }

    public static SSLContext buildJdkSSLContext(InputStream keyCertChainPathStream,
                                                InputStream privateKeyPathStream,
                                                InputStream trustCertStream, char[] password) {


        try {


            SSLContext sslContext = createSSLContext();

            // key manage factory
            KeyManagerFactory keyManagerFactory = null;
            if (keyCertChainPathStream == null) {
                keyManagerFactory = createKeyManagerFactory(privateKeyPathStream, password);
            } else {
                keyManagerFactory = createKeyManagerFactory(keyCertChainPathStream, privateKeyPathStream, password);
            }

            //trust manage factory
            TrustManagerFactory trustManagerFactory = createTrustManagerFactory(trustCertStream, password);

            TrustManager[] trustManagers = buildTrustManagers(trustManagerFactory);

            // init ssl context
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

            return sslContext;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", e);
        } finally {
            JdkSslUtils.safeCloseStream(trustCertStream);
            JdkSslUtils.safeCloseStream(keyCertChainPathStream);
            JdkSslUtils.safeCloseStream(privateKeyPathStream);
        }

    }

    public static KeyManagerFactory createKeyManagerFactory(InputStream keyCertChainPathStream, InputStream privateKeyPathStream, char[] keyPassword) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {

        return createKeyManagerFactory(Arrays.asList(IOUtils.toByteArray(keyCertChainPathStream)), IOUtils.toByteArray(privateKeyPathStream), keyPassword);
    }

    public static KeyManagerFactory createKeyManagerFactory(List<byte[]> keyCertChainInputStream, byte[] keyInputStream, char[] keyPassword) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {

        X509Certificate[] keyCertChain;

        try {
            keyCertChain = getCertificatesFromBuffers(keyCertChainInputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("Input stream not contain valid certificates.", e);
        }

        PrivateKey key;
        try {
            key = getPrivateKeyFromByteBuffer(keyInputStream, keyPassword);
        } catch (Exception e) {
            throw new IllegalArgumentException("Input stream does not contain valid private key.", e);
        }
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());

        keystore.load(null);


        keystore.setKeyEntry("keyEntry", key, keyPassword, keyCertChain);


        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        kmf.init(keystore, keyPassword);
        return kmf;
    }

    public static SSLContext createSSLContext() throws NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        return sslContext;
    }

    public static KeyStore createJdkKeyStore(InputStream privateKeyPathStream, char[] passwordCharArray) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(privateKeyPathStream, passwordCharArray);
        return keyStore;
    }

    public static TrustManagerFactory createTrustManagerFactory(InputStream trustCertStream, char[] passwordCharArray) throws Exception {
        if (trustCertStream == null) {
            return null;
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        // init trust manage
        KeyStore trustStore = createJdkKeyStore(trustCertStream, passwordCharArray);
        trustManagerFactory.init(trustStore);
        return trustManagerFactory;
    }

    public static KeyManagerFactory createKeyManagerFactory(InputStream privateKeyPathStream, char[] passwordCharArray) throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = createJdkKeyStore(privateKeyPathStream, passwordCharArray);
//            keyStore.setKeyEntry(ALIAS, key, passwordCharArray, certChain);
        keyManagerFactory.init(keyStore, passwordCharArray);
        return keyManagerFactory;
    }

    public static void safeCloseStream(Closeable stream) {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (IOException e) {
            logger.warn(TRANSPORT_FAILED_CLOSE_STREAM, "", "", "Failed to close a stream.", e);
        }
    }

    public static TrustManager[] buildTrustManagers(TrustManagerFactory trustManagerFactory) {

        if (trustManagerFactory != null) {
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers != null && trustManagers.length > 0) {

                return trustManagers;
            }
        }


        return null;
    }


    private static X509Certificate[] getCertificatesFromBuffers(List<byte[]> certs) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate[] x509Certs = new X509Certificate[certs.size()];
        for (int i = 0; i < certs.size(); i++) {
            InputStream is = new ByteArrayInputStream(certs.get(i));
            try {
                x509Certs[i] = (X509Certificate) cf.generateCertificate(is);
            } finally {
                JdkSslUtils.safeCloseStream(is);
            }
        }

        return x509Certs;
    }

    public static PrivateKey getPrivateKeyFromByteBuffer(byte[] encodedKey, char[] keyPassword)
        throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
        InvalidAlgorithmParameterException, KeyException, IOException {


        PKCS8EncodedKeySpec encodedKeySpec = generateKeySpec(
            keyPassword, encodedKey);
        try {
            return KeyFactory.getInstance("RSA").generatePrivate(encodedKeySpec);
        } catch (InvalidKeySpecException ignore) {
            try {
                return KeyFactory.getInstance("DSA").generatePrivate(encodedKeySpec);
            } catch (InvalidKeySpecException ignore2) {
                try {
                    return KeyFactory.getInstance("EC").generatePrivate(encodedKeySpec);
                } catch (InvalidKeySpecException e) {
                    throw new InvalidKeySpecException("Neither RSA, DSA nor EC worked", e);
                }
            }
        }
    }

    public static PKCS8EncodedKeySpec generateKeySpec(char[] password, byte[] key)
        throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
        InvalidKeyException, InvalidAlgorithmParameterException {

        if (password == null || password.length == 0) {
            return new PKCS8EncodedKeySpec(key);
        }

        EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new EncryptedPrivateKeyInfo(key);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(encryptedPrivateKeyInfo.getAlgName());
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
        SecretKey pbeKey = keyFactory.generateSecret(pbeKeySpec);

        Cipher cipher = Cipher.getInstance(encryptedPrivateKeyInfo.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, encryptedPrivateKeyInfo.getAlgParameters());

        return encryptedPrivateKeyInfo.getKeySpec(cipher);
    }

    public static char[] strPasswordToCharArray(String password) {
        return password == null ? new char[0] : password.toCharArray();
    }


}
