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
package org.apache.dubbo.security.cert;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.security.cert.impl.AuthorityServiceImpl;
import org.apache.dubbo.security.cert.impl.RuleServiceImpl;

import io.grpc.Grpc;
import io.grpc.Server;
import io.grpc.TlsServerCredentials;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class MockServer {

    private final Server server;
    private final CertUtils.KeyPair keyPair;
    private final X509Certificate certificate;
    private final AuthorityServiceImpl authorityService;
    private final RuleServiceImpl ruleService;

    public MockServer(Server server, CertUtils.KeyPair keyPair, X509Certificate certificate, AuthorityServiceImpl authorityService, RuleServiceImpl ruleService) {
        this.server = server;
        this.keyPair = keyPair;
        this.certificate = certificate;
        this.authorityService = authorityService;
        this.ruleService = ruleService;
    }

    public Server getServer() {
        return server;
    }

    public CertUtils.KeyPair getKeyPair() {
        return keyPair;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public AuthorityServiceImpl getAuthorityService() {
        return authorityService;
    }

    public RuleServiceImpl getRuleService() {
        return ruleService;
    }

    public static MockServer startServer() throws CertificateException, IOException {
        int port = NetUtils.getAvailablePort();

        CertUtils.KeyPair keyPair = CertUtils.signWithEcdsa();
        X509Certificate certificate = generate(keyPair);
        String privateKey = CertUtils.generatePemKey("EC PRIVATE KEY", keyPair.getPrivateKey().getEncoded());
        String pemCert = CertUtils.generatePemKey("CERTIFICATE", certificate.getEncoded());

        AuthorityServiceImpl authorityService = new AuthorityServiceImpl();
        RuleServiceImpl ruleService = new RuleServiceImpl();
        Server grpcServer = Grpc.newServerBuilderForPort(port, TlsServerCredentials
                .newBuilder()
                .keyManager(new ByteArrayInputStream(pemCert.getBytes()), new ByteArrayInputStream(privateKey.getBytes()))
                .trustManager(InsecureTrustManagerFactory.INSTANCE.getTrustManagers())
                .build())
            .addService(authorityService)
            .addService(ruleService)
            .build()
            .start();
        return new MockServer(grpcServer, keyPair, certificate, authorityService, ruleService);
    }

    public static X509Certificate generate(CertUtils.KeyPair keyPair)
        throws CertificateException, CertIOException {
        final Instant now = Instant.now();
        final Date notBefore = Date.from(now);
        final Date notAfter = Date.from(now.plus(Duration.ofDays(1)));
        final X500Name x500Name = new X500Name("CN=Test");
        ASN1Encodable[] subjectAlternativeNames = new ASN1Encodable[]{new GeneralName(GeneralName.dNSName, "localhost")};
        final X509v3CertificateBuilder certificateBuilder =
            new JcaX509v3CertificateBuilder(x500Name,
                BigInteger.valueOf(now.toEpochMilli()),
                notBefore,
                notAfter,
                x500Name,
                keyPair.getPublicKey())
                .addExtension(Extension.subjectAlternativeName, false, new DERSequence(subjectAlternativeNames))
                .addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        return new JcaX509CertificateConverter()
            .setProvider(new BouncyCastleProvider()).getCertificate(certificateBuilder.build(keyPair.getSigner()));
    }

}
