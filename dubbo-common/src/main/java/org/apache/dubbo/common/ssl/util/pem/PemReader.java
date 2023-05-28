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

import org.apache.dubbo.common.ssl.util.JdkSslUtils;
import org.apache.dubbo.config.Constants;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;

/**
 * for read .pem certificate
 */
public class PemReader {


    public static List<byte[]> readCertificates(File file) throws CertificateException {
        try {
            InputStream in = new FileInputStream(file);

            try {
                return readCertificates(in);
            } finally {
                JdkSslUtils.safeCloseStream(in);
            }
        } catch (FileNotFoundException e) {
            throw new CertificateException("could not find certificate file: " + file);
        }
    }

    public static List<byte[]> readCertificates(InputStream in) throws CertificateException {
        String content;
        try {
            content = readContent(in);
        } catch (IOException e) {
            throw new CertificateException("failed to read certificate input stream", e);
        }

        List<byte[]> certs = new ArrayList<byte[]>();
        Matcher m = Constants.CERT_HEADER.matcher(content);
        int start = 0;
        for (; ; ) {
            if (!m.find(start)) {
                break;
            }
            m.usePattern(Constants.BODY);
            if (!m.find()) {
                break;
            }

            byte[] der = null;
            try {
                der = decode(m.group(0));
            } catch (Exception e) {

            }

            m.usePattern(Constants.CERT_FOOTER);
            if (!m.find()) {
                // Certificate is incomplete.
                break;
            }

            if (der != null) {
                certs.add(der);
            }


            start = m.end();
            m.usePattern(Constants.CERT_HEADER);
        }

        if (certs.isEmpty()) {
            throw new CertificateException("found no certificates in input stream");
        }

        return certs;
    }

    public static byte[] readPrivateKey(File file) throws KeyException {
        try {
            InputStream in = new FileInputStream(file);

            try {
                return readPrivateKey(in);
            } finally {
                JdkSslUtils.safeCloseStream(in);
            }
        } catch (FileNotFoundException e) {
            throw new KeyException("could not find key file: " + file);
        }
    }

    public static byte[] readPrivateKey(InputStream in) throws KeyException {
        String content;
        try {
            content = readContent(in);
        } catch (IOException e) {
            throw new KeyException("failed to read key input stream", e);
        }

        Matcher m = Constants.KEY_HEADER.matcher(content);
        if (!m.find()) {
            throw keyNotFoundException();
        }
        m.usePattern(Constants.BODY);
        if (!m.find()) {
            throw keyNotFoundException();
        }
        byte[] privateKey = null;
        try {
            privateKey = decode(m.group(0));
        } catch (Exception e) {
            return null;
        }

        m.usePattern(Constants.KEY_FOOTER);
        if (!m.find()) {
            // Key is incomplete.
            throw keyNotFoundException();
        }

        return privateKey;

    }

    private static KeyException keyNotFoundException() {
        return new KeyException("could not find a PKCS  private key in input stream");
    }

    private static String readContent(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[8192];
            for (; ; ) {
                int ret = in.read(buf);
                if (ret < 0) {
                    break;
                }
                out.write(buf, 0, ret);
            }
            return out.toString("US-ASCII");
        } finally {
            JdkSslUtils.safeCloseStream(in);
            JdkSslUtils.safeCloseStream(out);
        }
    }

    private static byte[] decode(String str) {

        return Base64.getMimeDecoder().decode(str.getBytes(StandardCharsets.US_ASCII));
    }
}
