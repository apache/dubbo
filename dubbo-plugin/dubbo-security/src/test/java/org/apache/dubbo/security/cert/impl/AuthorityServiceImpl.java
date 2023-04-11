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
package org.apache.dubbo.security.cert.impl;

import org.apache.dubbo.auth.v1alpha1.AuthorityServiceGrpc;
import org.apache.dubbo.auth.v1alpha1.IdentityRequest;
import org.apache.dubbo.auth.v1alpha1.IdentityResponse;

import io.grpc.stub.StreamObserver;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import sun.security.pkcs10.PKCS10;
import sun.security.provider.X509Factory;
import sun.security.util.DerValue;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.KeyUsageExtension;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import java.io.IOException;
import java.io.PrintStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

public class AuthorityServiceImpl extends AuthorityServiceGrpc.AuthorityServiceImplBase {
    private final AtomicReference<IdentityResponse> responseRef = new AtomicReference<>();
    private final AtomicReference<IdentityRequest> requestRef = new AtomicReference<>();

    @Override
    public void createIdentity(IdentityRequest request, StreamObserver<IdentityResponse> responseObserver) {
        requestRef.set(request);

        responseObserver.onNext(responseRef.get());
        responseObserver.onCompleted();
    }

    public static byte[] sign(PKCS10 csr, X509CertImpl signerCert, PrivateKey signerPrivKey) throws CertificateException, IOException, InvalidKeyException, SignatureException {
        /*
         * The code below is partly taken from the KeyTool class in OpenJDK7.
         */
        X509CertInfo signerCertInfo = (X509CertInfo) signerCert.get(X509CertImpl.NAME + "." + X509CertImpl.INFO);
        X500Name issuer = (X500Name) signerCertInfo.get(X509CertInfo.SUBJECT + "." + CertificateSubjectName.DN_NAME);

        /*
         * Set the certificate's validity:
         * From now and for VALIDITY_DAYS days
         */
        Date firstDate = new Date();
        Date lastDate = new Date();
        lastDate.setTime(firstDate.getTime() + VALIDITY_DAYS * 1000L * 24L * 60L * 60L);
        CertificateValidity interval = new CertificateValidity(firstDate, lastDate);

        /*
         * Initialize the signature object
         */
        Signature signature;
        try {
            signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        signature.initSign(signerPrivKey);

        // create the certificate info
        X509CertInfo certInfo = new X509CertInfo();
        certInfo.set(X509CertInfo.VALIDITY, interval);
        certInfo.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new Random().nextInt() & 0x7fffffff));
        certInfo.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        try {
            certInfo.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(AlgorithmId.get(SIGNATURE_ALGORITHM)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        certInfo.set(X509CertInfo.ISSUER, issuer);
        certInfo.set(X509CertInfo.KEY, new CertificateX509Key(csr.getSubjectPublicKeyInfo()));
        certInfo.set(X509CertInfo.SUBJECT, csr.getSubjectName());

        /*
         * Add x509v3 extensions to the container
         */
        CertificateExtensions extensions = new CertificateExtensions();

        // Example extension.
        // See KeyTool source for more.
        boolean[] keyUsagePolicies = new boolean[9];
        keyUsagePolicies[0] = true; // Digital Signature
        keyUsagePolicies[2] = true; // Key encipherment
        KeyUsageExtension kue = new KeyUsageExtension(keyUsagePolicies);
        byte[] keyUsageValue = new DerValue(DerValue.tag_OctetString, kue.getExtensionValue()).toByteArray();
        extensions.set(KeyUsageExtension.NAME, new Extension(
            kue.getExtensionId(),
            true, // Critical
            keyUsageValue));

        /*
         * Create the certificate and sign it
         */
        X509CertImpl cert = new X509CertImpl(certInfo);
        try {
            cert.sign(signerPrivKey, SIGNATURE_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }

        /*
         * Return the signed certificate as PEM-encoded bytes
         */
        ByteOutputStream bos = new ByteOutputStream();
        PrintStream out = new PrintStream(bos);
        BASE64Encoder encoder = new BASE64Encoder();
        out.println(X509Factory.BEGIN_CERT);
        encoder.encodeBuffer(cert.getEncoded(), out);
        out.println(X509Factory.END_CERT);
        out.flush();
        return bos.getBytes();
    }

    public AtomicReference<IdentityResponse> getResponseRef() {
        return responseRef;
    }

    public AtomicReference<IdentityRequest> getRequestRef() {
        return requestRef;
    }
}
