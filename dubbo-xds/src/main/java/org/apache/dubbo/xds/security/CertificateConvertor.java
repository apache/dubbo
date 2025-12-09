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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;

public class CertificateConvertor {

    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

    public static List<X509Certificate> readPemX509CertificateChains(List<String> x590CertChains)
            throws IOException, CertificateException {
        List<String> certs = new ArrayList<>();

        for (String certChain : x590CertChains) {
            String[] split = certChain.split(END_CERTIFICATE);
            for (String c : split) {
                certs.add(c + END_CERTIFICATE);
            }
        }
        return readPemX509Certificates(certs);
    }

    public static List<X509Certificate> readPemX509Certificates(List<String> x509Certs)
            throws IOException, CertificateException {
        List<X509Certificate> certs = new ArrayList<>();
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();

        for (String cert : x509Certs) {
            X509CertificateHolder holder = readX509Certificate(cert);
            certs.add(converter.getCertificate(holder));
        }
        return certs;
    }

    public static X509CertificateHolder readX509Certificate(File x509Cert) throws IOException {
        PEMParser pemParser = new PEMParser(new FileReader(x509Cert));
        return (X509CertificateHolder) pemParser.readObject();
    }

    public static X509CertificateHolder readX509Certificate(String x509Cert) throws IOException {
        PEMParser pemParser = new PEMParser(new StringReader(x509Cert));
        return (X509CertificateHolder) pemParser.readObject();
    }
}
