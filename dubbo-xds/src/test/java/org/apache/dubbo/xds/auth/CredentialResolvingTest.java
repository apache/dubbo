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
package org.apache.dubbo.xds.auth;

import org.apache.dubbo.xds.security.api.X509CertChains;
import org.apache.dubbo.xds.security.authz.resolver.ConnectionCredentialResolver.CertificateCredential;
import org.apache.dubbo.xds.security.authz.resolver.ConnectionCredentialResolver.ConnectionCredential;
import org.apache.dubbo.xds.security.authz.resolver.SpiffeCredentialResolver;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class CredentialResolvingTest {

    @Test
    public void testResolveSpiffe() throws Exception {
        SpiffeCredentialResolver spiffeCredentialResolver = new SpiffeCredentialResolver();
        URI spiffeId =
                spiffeCredentialResolver.readSpiffeId(getConnectionCredential().getCertificateCredentials());
    }

    private ConnectionCredential getConnectionCredential() throws Exception {
        List<CertificateCredential> certs = new ArrayList<>();
        CertificateCredential certificateCredential = new CertificateCredential(new X509CertChains(Collections.singletonList(testCert)).readAsCerts().get(0));
        certs.add(certificateCredential);
        ConnectionCredential connectionCredential = new ConnectionCredential(certs, "http/2", "my.application.app1");
        return connectionCredential;
    }

    /**
     *Certificate:
     *     Data:
     *         Version: 3 (0x2)
     *         Serial Number:
     *             04:10:df:a2:3b:45:3b:75:ec:fd:fa:41:1b:84:cb:70:d9
     *     Signature Algorithm: sha256WithRSAEncryption
     *         Issuer: CN=SPIFFE CA
     *         Validity
     *             Not Before: Mar 10 12:00:00 2021 GMT
     *             Not After : Mar 10 12:00:00 2022 GMT
     *         Subject: CN=spiffe://cluster.local/ns/default/sa/service1
     *         Subject Public Key Info:
     *             Public Key Algorithm: rsaEncryption
     *                 Public-Key: (2048 bit)
     *         X509v3 extensions:
     *             X509v3 Key Usage: critical
     *                 Digital Signature, Key Encipherment
     *             X509v3 Extended Key Usage:
     *                 TLS Web Server Authentication, TLS Web Client Authentication
     *             X509v3 Subject Alternative Name:
     *                 URI: spiffe://cluster.local/ns/default/sa/service1
     *     Signature Algorithm: sha256WithRSAEncryption
     *          5c:ca:ba:8e:92:...:00:00:00:00:00:00:00:00:00:00:00:00
     */
    final String testCert = "-----BEGIN CERTIFICATE-----\n"
            + "Q2VydGlmaWNhdGU6DQogICAgRGF0YToNCiAgICAgICAgVmVyc2lvbjogMyAoMHgy\n"
            + "KQ0KICAgICAgICBTZXJpYWwgTnVtYmVyOg0KICAgICAgICAgICAgMDQ6MTA6ZGY6\n"
            + "YTI6M2I6NDU6M2I6NzU6ZWM6ZmQ6ZmE6NDE6MWI6ODQ6Y2I6NzA6ZDkNCiAgICBT\n"
            + "aWduYXR1cmUgQWxnb3JpdGhtOiBzaGEyNTZXaXRoUlNBRW5jcnlwdGlvbg0KICAg\n"
            + "ICAgICBJc3N1ZXI6IENOPVNQSUZGRSBDQQ0KICAgICAgICBWYWxpZGl0eQ0KICAg\n"
            + "ICAgICAgICAgTm90IEJlZm9yZTogTWFyIDEwIDEyOjAwOjAwIDIwMjEgR01UDQog\n"
            + "ICAgICAgICAgICBOb3QgQWZ0ZXIgOiBNYXIgMTAgMTI6MDA6MDAgMjAyMiBHTVQN\n"
            + "CiAgICAgICAgU3ViamVjdDogQ049c3BpZmZlOi8vY2x1c3Rlci5sb2NhbC9ucy9k\n"
            + "ZWZhdWx0L3NhL3NlcnZpY2UxIA0KICAgICAgICBTdWJqZWN0IFB1YmxpYyBLZXkg\n"
            + "SW5mbzoNCiAgICAgICAgICAgIFB1YmxpYyBLZXkgQWxnb3JpdGhtOiByc2FFbmNy\n"
            + "eXB0aW9uDQogICAgICAgICAgICAgICAgUHVibGljLUtleTogKDIwNDggYml0KQ0K\n"
            + "ICAgICAgICBYNTA5djMgZXh0ZW5zaW9uczoNCiAgICAgICAgICAgIFg1MDl2MyBL\n"
            + "ZXkgVXNhZ2U6IGNyaXRpY2FsDQogICAgICAgICAgICAgICAgRGlnaXRhbCBTaWdu\n"
            + "YXR1cmUsIEtleSBFbmNpcGhlcm1lbnQNCiAgICAgICAgICAgIFg1MDl2MyBFeHRl\n"
            + "bmRlZCBLZXkgVXNhZ2U6IA0KICAgICAgICAgICAgICAgIFRMUyBXZWIgU2VydmVy\n"
            + "IEF1dGhlbnRpY2F0aW9uLCBUTFMgV2ViIENsaWVudCBBdXRoZW50aWNhdGlvbg0K\n"
            + "ICAgICAgICAgICAgWDUwOXYzIFN1YmplY3QgQWx0ZXJuYXRpdmUgTmFtZTogDQog\n"
            + "ICAgICAgICAgICAgICAgVVJJOiBzcGlmZmU6Ly9jbHVzdGVyLmxvY2FsL25zL2Rl\n"
            + "ZmF1bHQvc2Evc2VydmljZTEgDQogICAgU2lnbmF0dXJlIEFsZ29yaXRobTogc2hh\n"
            + "MjU2V2l0aFJTQUVuY3J5cHRpb24NCiAgICAgICAgIDVjOmNhOmJhOjhlOjkyOi4u\n"
            + "LjowMDowMDowMDowMDowMDowMDowMDowMDowMDowMDowMDowMA=="
            + "-----END CERTIFICATE-----";
}
