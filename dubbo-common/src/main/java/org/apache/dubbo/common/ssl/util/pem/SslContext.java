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
package org.apache.dubbo.common.ssl.util.pem;


import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
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
import java.util.List;

public class SslContext {

    /**
     * Generates a key specification for an (encrypted) private key.
     *
     * @param password characters, if {@code null} an unencrypted key is assumed
     * @param key      bytes of the DER encoded private key
     * @return a key specification
     * @throws IOException                        if parsing {@code key} fails
     * @throws NoSuchAlgorithmException           if the algorithm used to encrypt {@code key} is unknown
     * @throws NoSuchPaddingException             if the padding scheme specified in the decryption algorithm is unknown
     * @throws InvalidKeySpecException            if the decryption key based on {@code password} cannot be generated
     * @throws InvalidKeyException                if the decryption key based on {@code password} cannot be used to decrypt
     *                                            {@code key}
     * @throws InvalidAlgorithmParameterException if decryption algorithm parameters are somehow faulty
     */
    protected static PKCS8EncodedKeySpec generateKeySpec(char[] password, byte[] key)
        throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
        InvalidKeyException, InvalidAlgorithmParameterException {

        if (password == null) {
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

    /**
     * Generates a new {@link KeyStore}.
     *
     * @param certChain        an X.509 certificate chain
     * @param key              a PKCS#8 private key
     * @param keyPasswordChars the password of the {@code keyFile}.
     *                         {@code null} if it's not password-protected.
     * @param keyStoreType     The KeyStore Type you want to use
     * @return generated {@link KeyStore}.
     */
    protected static KeyStore buildKeyStore(X509Certificate[] certChain, PrivateKey key,
                                            char[] keyPasswordChars, String keyStoreType)
        throws KeyStoreException, NoSuchAlgorithmException,
        CertificateException, IOException {
        if (keyStoreType == null) {
            keyStoreType = KeyStore.getDefaultType();
        }
        KeyStore ks = KeyStore.getInstance(keyStoreType);
        ks.load(null, null);
        ks.setKeyEntry("keyEntry", key, keyPasswordChars, certChain);
        return ks;
    }

    protected static PrivateKey toPrivateKey(File keyFile, String keyPassword) throws NoSuchAlgorithmException,
        NoSuchPaddingException, InvalidKeySpecException,
        InvalidAlgorithmParameterException,
        KeyException, IOException {
        if (keyFile == null) {
            return null;
        }

        return getPrivateKeyFromByteBuffer(PemReader.readPrivateKey(keyFile), keyPassword);
    }

    protected static PrivateKey toPrivateKey(InputStream keyInputStream, String keyPassword)
        throws NoSuchAlgorithmException,
        NoSuchPaddingException, InvalidKeySpecException,
        InvalidAlgorithmParameterException,
        KeyException, IOException {
        if (keyInputStream == null) {
            return null;
        }

        return getPrivateKeyFromByteBuffer(PemReader.readPrivateKey(keyInputStream), keyPassword);
    }

    private static PrivateKey getPrivateKeyFromByteBuffer(byte[] encodedKey, String keyPassword)
        throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException,
        InvalidAlgorithmParameterException, KeyException, IOException {


        PKCS8EncodedKeySpec encodedKeySpec = generateKeySpec(
            keyPassword == null ? null : keyPassword.toCharArray(), encodedKey);
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

    /**
     * Build a {@link TrustManagerFactory} from a certificate chain file.
     *
     * @param certChainFile       The certificate file to build from.
     * @param trustManagerFactory The existing {@link TrustManagerFactory} that will be used if not {@code null}.
     * @return A {@link TrustManagerFactory} which contains the certificates in {@code certChainFile}
     */
    @Deprecated
    protected static TrustManagerFactory buildTrustManagerFactory(
        File certChainFile, TrustManagerFactory trustManagerFactory)
        throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        return buildTrustManagerFactory(certChainFile, trustManagerFactory, null);
    }

    /**
     * Build a {@link TrustManagerFactory} from a certificate chain file.
     *
     * @param certChainFile       The certificate file to build from.
     * @param trustManagerFactory The existing {@link TrustManagerFactory} that will be used if not {@code null}.
     * @param keyType             The KeyStore Type you want to use
     * @return A {@link TrustManagerFactory} which contains the certificates in {@code certChainFile}
     */
    protected static TrustManagerFactory buildTrustManagerFactory(
        File certChainFile, TrustManagerFactory trustManagerFactory, String keyType)
        throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        X509Certificate[] x509Certs = toX509Certificates(certChainFile);

        return buildTrustManagerFactory(x509Certs, trustManagerFactory, keyType);
    }

    public static X509Certificate[] toX509Certificates(File file) throws CertificateException {
        if (file == null) {
            return null;
        }
        return getCertificatesFromBuffers(PemReader.readCertificates(file));
    }

    protected static X509Certificate[] toX509Certificates(InputStream in) throws CertificateException {
        if (in == null) {
            return null;
        }
        return getCertificatesFromBuffers(PemReader.readCertificates(in));
    }

    private static X509Certificate[] getCertificatesFromBuffers(List<byte[]> certs) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate[] x509Certs = new X509Certificate[certs.size()];

        try {
            for (int i = 0; i < certs.size(); i++) {
                InputStream is = new ByteArrayInputStream(certs.get(i));
                try {
                    x509Certs[i] = (X509Certificate) cf.generateCertificate(is);
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // This is not expected to happen, but re-throw in case it does.
                        throw new RuntimeException(e);
                    }
                }
            }
        } finally {
        }
        return x509Certs;
    }

    public static TrustManagerFactory buildTrustManagerFactory(
        X509Certificate[] certCollection, TrustManagerFactory trustManagerFactory, String keyStoreType)
        throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
        if (keyStoreType == null) {
            keyStoreType = KeyStore.getDefaultType();
        }
        final KeyStore ks = KeyStore.getInstance(keyStoreType);
        ks.load(null, null);

        int i = 1;
        for (X509Certificate cert : certCollection) {
            String alias = Integer.toString(i);
            ks.setCertificateEntry(alias, cert);
            i++;
        }

        // Set up trust manager factory to use our key store.
        if (trustManagerFactory == null) {
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        }
        trustManagerFactory.init(ks);

        return trustManagerFactory;
    }

    static PrivateKey toPrivateKeyInternal(File keyFile, String keyPassword) throws SSLException {
        try {
            return toPrivateKey(keyFile, keyPassword);
        } catch (Exception e) {
            throw new SSLException(e);
        }
    }

    static X509Certificate[] toX509CertificatesInternal(File file) throws SSLException {
        try {
            return toX509Certificates(file);
        } catch (CertificateException e) {
            throw new SSLException(e);
        }
    }

    protected static KeyManagerFactory buildKeyManagerFactory(X509Certificate[] certChainFile,
                                                              String keyAlgorithm, PrivateKey key,
                                                              String keyPassword, KeyManagerFactory kmf,
                                                              String keyStore)
        throws KeyStoreException, NoSuchAlgorithmException, IOException,
        CertificateException, UnrecoverableKeyException {
        if (keyAlgorithm == null) {
            keyAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
        }
        char[] keyPasswordChars = keyStorePassword(keyPassword);
        KeyStore ks = buildKeyStore(certChainFile, key, keyPasswordChars, keyStore);
        return buildKeyManagerFactory(ks, keyAlgorithm, keyPasswordChars, kmf);
    }

    static KeyManagerFactory buildKeyManagerFactory(KeyStore ks,
                                                    String keyAlgorithm,
                                                    char[] keyPasswordChars, KeyManagerFactory kmf)
        throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        // Set up key manager factory to use our key store
        if (kmf == null) {
            if (keyAlgorithm == null) {
                keyAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
            }
            kmf = KeyManagerFactory.getInstance(keyAlgorithm);
        }
        kmf.init(ks, keyPasswordChars);

        return kmf;
    }

    static char[] keyStorePassword(String keyPassword) {
        return keyPassword == null ? new char[0] : keyPassword.toCharArray();
    }
}
