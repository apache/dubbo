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
package org.apache.dubbo.xds.security;

import org.apache.dubbo.xds.bootstrap.Bootstrapper.CertificateProviderInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

/** Validated snapshots tolerate non-atomic agent file updates without publishing a mismatched key pair. */
public final class CertificateFiles {
    private static final int MAX_FILE_BYTES = 1024 * 1024;
    private final Path certificate;
    private final Path privateKey;
    private final Path roots;
    private volatile Material material;
    private volatile String lastError;
    private byte[] fingerprint;
    private Long identityNotBeforeMillis;
    private Long identityNotAfterMillis;
    private Long lastSuccessfulRefreshMillis;

    CertificateFiles(CertificateProviderInfo provider) {
        certificate = path(provider, "certificate_file");
        privateKey = path(provider, "private_key_file");
        roots = path(provider, "ca_certificate_file");
    }

    private static Path path(CertificateProviderInfo provider, String key) {
        return provider.getConfig().containsFields(key)
                ? Paths.get(provider.getConfig().getFieldsOrThrow(key).getStringValue())
                : null;
    }

    synchronized boolean refresh() {
        try {
            byte[] chain = read(certificate);
            byte[] key = read(privateKey);
            byte[] trust = read(roots);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (byte[] bytes : new byte[][] {chain, key, trust}) {
                if (bytes != null) {
                    digest.update(bytes);
                }
            }
            byte[] next = digest.digest();
            if (Arrays.equals(next, fingerprint)) {
                checkValidity(material);
                lastError = null;
                return false;
            }
            X509Certificate leaf = null;
            if (chain != null) {
                leaf = (X509Certificate)
                        CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(chain));
                leaf.checkValidity();
                key = validatePair(leaf, key);
            }
            if (trust != null) {
                Collection<? extends Certificate> authorities =
                        CertificateFactory.getInstance("X.509").generateCertificates(new ByteArrayInputStream(trust));
                if (authorities.isEmpty()) {
                    throw new IOException("CA bundle is empty");
                }
            }
            material = new Material(chain, key, trust, leaf, hex(next));
            identityNotBeforeMillis = leaf == null ? null : leaf.getNotBefore().getTime();
            identityNotAfterMillis = leaf == null ? null : leaf.getNotAfter().getTime();
            lastSuccessfulRefreshMillis = System.currentTimeMillis();
            fingerprint = next;
            lastError = null;
            return true;
        } catch (Exception error) {
            lastError = error.getClass().getSimpleName();
            // Retain only a still-valid, previously coherent snapshot.
            try {
                checkValidity(material);
            } catch (Exception invalid) {
                material = null;
            }
            return false;
        }
    }

    public Material current() {
        Material result = material;
        if (result == null) {
            throw new IllegalStateException("Certificate files have no valid snapshot: " + lastError);
        }
        try {
            checkValidity(result);
        } catch (Exception expired) {
            throw new IllegalStateException("Certificate snapshot has expired", expired);
        }
        return result;
    }

    public String getLastError() {
        return lastError;
    }

    /** Observes accepted material only; does not read files or expose paths, identity or key-derived hashes. */
    public synchronized Status inspect() {
        long now = System.currentTimeMillis();
        Status.State state;
        if (material == null) {
            state = lastError == null ? Status.State.NOT_LOADED : Status.State.UNAVAILABLE;
        } else if (identityNotAfterMillis != null && now > identityNotAfterMillis) {
            state = Status.State.EXPIRED;
        } else if (identityNotBeforeMillis != null && now < identityNotBeforeMillis) {
            state = Status.State.NOT_YET_VALID;
        } else {
            state = Status.State.READY;
        }
        return new Status(
                state, now, identityNotBeforeMillis, identityNotAfterMillis, lastSuccessfulRefreshMillis, lastError);
    }

    public static final class Status {
        public enum State {
            NOT_LOADED,
            READY,
            EXPIRED,
            NOT_YET_VALID,
            UNAVAILABLE
        }

        private final State state;
        private final long observedAtMillis;
        private final Long identityNotBeforeMillis;
        private final Long identityNotAfterMillis;
        private final Long lastSuccessfulRefreshMillis;
        private final String lastRefreshError;

        private Status(
                State state,
                long observedAtMillis,
                Long identityNotBeforeMillis,
                Long identityNotAfterMillis,
                Long lastSuccessfulRefreshMillis,
                String lastRefreshError) {
            this.state = state;
            this.observedAtMillis = observedAtMillis;
            this.identityNotBeforeMillis = identityNotBeforeMillis;
            this.identityNotAfterMillis = identityNotAfterMillis;
            this.lastSuccessfulRefreshMillis = lastSuccessfulRefreshMillis;
            this.lastRefreshError = lastRefreshError;
        }

        public State getState() {
            return state;
        }

        public long getObservedAtMillis() {
            return observedAtMillis;
        }

        /** Last accepted leaf validity; null for a roots-only or never-loaded provider. */
        public Long getIdentityNotBeforeMillis() {
            return identityNotBeforeMillis;
        }

        public Long getIdentityNotAfterMillis() {
            return identityNotAfterMillis;
        }

        /** Time when changed coherent material was accepted, not the last unchanged poll. */
        public Long getLastSuccessfulRefreshMillis() {
            return lastSuccessfulRefreshMillis;
        }

        public String getLastRefreshError() {
            return lastRefreshError;
        }

        static Status notLoaded() {
            return new Status(State.NOT_LOADED, System.currentTimeMillis(), null, null, null, null);
        }
    }

    private static void checkValidity(Material value) throws Exception {
        if (value == null) {
            throw new IOException("No certificate material");
        }
        if (value.leaf != null) {
            value.leaf.checkValidity();
        }
    }

    private static byte[] read(Path path) throws IOException {
        if (path == null) {
            return null;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MAX_FILE_BYTES) {
                    throw new IOException("Certificate file exceeds size limit");
                }
                output.write(buffer, 0, count);
            }
        }
        byte[] content = output.toByteArray();
        if (content.length == 0 || content.length > MAX_FILE_BYTES) {
            throw new IOException("Invalid certificate file size");
        }
        return content;
    }

    private static byte[] validatePair(X509Certificate leaf, byte[] pem) throws Exception {
        if (pem == null) {
            throw new IOException("Missing private key");
        }
        String text = new String(pem, StandardCharsets.US_ASCII);
        boolean pkcs1 = text.contains("-----BEGIN RSA PRIVATE KEY-----");
        String begin = pkcs1 ? "-----BEGIN RSA PRIVATE KEY-----" : "-----BEGIN PRIVATE KEY-----";
        String end = pkcs1 ? "-----END RSA PRIVATE KEY-----" : "-----END PRIVATE KEY-----";
        int start = text.indexOf(begin);
        int stop = text.indexOf(end);
        if (start < 0 || stop < start) {
            throw new IOException("Unsupported private key encoding");
        }
        byte[] der = Base64.getMimeDecoder().decode(text.substring(start + begin.length(), stop));
        if (pkcs1) {
            byte[] rsaAlgorithm = new byte[] {
                0x30,
                0x0d,
                0x06,
                0x09,
                0x2a,
                (byte) 0x86,
                0x48,
                (byte) 0x86,
                (byte) 0xf7,
                0x0d,
                0x01,
                0x01,
                0x01,
                0x05,
                0x00
            };
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            body.write(new byte[] {0x02, 0x01, 0x00});
            body.write(rsaAlgorithm);
            body.write(der(0x04, der));
            der = der(0x30, body.toByteArray());
        }
        String algorithm = leaf.getPublicKey().getAlgorithm();
        PrivateKey key = KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(der));
        String signatureAlgorithm;
        switch (algorithm) {
            case "RSA":
                signatureAlgorithm = "SHA256withRSA";
                break;
            case "EC":
                signatureAlgorithm = "SHA256withECDSA";
                break;
            case "DSA":
                signatureAlgorithm = "SHA256withDSA";
                break;
            case "Ed25519":
                signatureAlgorithm = "Ed25519";
                break;
            default:
                throw new IOException("Unsupported certificate key algorithm");
        }
        byte[] challenge = "dubbo-xds-key-pair-validation".getBytes(StandardCharsets.US_ASCII);
        Signature signature = Signature.getInstance(signatureAlgorithm);
        signature.initSign(key);
        signature.update(challenge);
        byte[] signed = signature.sign();
        signature.initVerify(leaf.getPublicKey());
        signature.update(challenge);
        if (!signature.verify(signed)) {
            throw new IOException("Certificate and private key do not match");
        }
        // Netty's JDK key parser expects PKCS#8, including when the agent supplied PKCS#1 RSA.
        return ("-----BEGIN PRIVATE KEY-----\n"
                        + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(der)
                        + "\n-----END PRIVATE KEY-----\n")
                .getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] der(int tag, byte[] value) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        result.write(tag);
        if (value.length < 128) {
            result.write(value.length);
        } else {
            int lengthBytes = (32 - Integer.numberOfLeadingZeros(value.length) + 7) / 8;
            result.write(0x80 | lengthBytes);
            for (int i = lengthBytes - 1; i >= 0; i--) {
                result.write(value.length >>> (8 * i));
            }
        }
        result.write(value);
        return result.toByteArray();
    }

    static String hex(byte[] bytes) {
        StringBuilder text = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            text.append(Character.forDigit((value >> 4) & 15, 16)).append(Character.forDigit(value & 15, 16));
        }
        return text.toString();
    }

    public static final class Material {
        final byte[] chain;
        final byte[] key;
        final byte[] roots;
        final X509Certificate leaf;
        private final String fingerprint;

        Material(byte[] chain, byte[] key, byte[] roots, X509Certificate leaf, String fingerprint) {
            this.chain = chain;
            this.key = key;
            this.roots = roots;
            this.leaf = leaf;
            this.fingerprint = fingerprint;
        }

        public String getFingerprint() {
            return fingerprint;
        }
    }
}
