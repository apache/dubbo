/*
 * Copyright 2019 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.CharsetUtil;

/**
 * Contains certificate utility method(s).
 */
public final class CertificateUtils {
  private static final Logger logger = Logger.getLogger(CertificateUtils.class.getName());

  private static CertificateFactory factory;
  private static final Pattern KEY_PATTERN = Pattern.compile(
          "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+"  // Header
                  + "([a-z0-9+/=\\r\\n]+)"                             // Base64 text
                  + "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+",                  // Footer
          Pattern.CASE_INSENSITIVE);

  private static synchronized void initInstance() throws CertificateException {
    if (factory == null) {
      factory = CertificateFactory.getInstance("X.509");
    }
  }

  /**
   * Generates X509Certificate array from a file on disk.
   *
   * @param file a {@link File} containing the cert data
   */
  static X509Certificate[] toX509Certificates(File file) throws CertificateException, IOException {
    try (FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis)) {
      return toX509Certificates(bis);
    }
  }

  /** Generates X509Certificate array from the {@link InputStream}. */
  public static synchronized X509Certificate[] toX509Certificates(InputStream inputStream)
      throws CertificateException, IOException {
    initInstance();
    Collection<? extends Certificate> certs = factory.generateCertificates(inputStream);
    return certs.toArray(new X509Certificate[0]);

  }

  /** See {@link CertificateFactory#generateCertificate(InputStream)}. */
  public static synchronized X509Certificate toX509Certificate(InputStream inputStream)
          throws CertificateException, IOException {
    initInstance();
    Certificate cert = factory.generateCertificate(inputStream);
    return (X509Certificate) cert;
  }

  /** Generates a {@link PrivateKey} from the {@link InputStream}. */
  public static PrivateKey getPrivateKey(InputStream inputStream)
          throws Exception {
    ByteBuf encodedKeyBuf = readPrivateKey(inputStream);
    byte[] encodedKey = new byte[encodedKeyBuf.readableBytes()];
    encodedKeyBuf.readBytes(encodedKey).release();
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encodedKey);
    return KeyFactory.getInstance("RSA").generatePrivate(spec);
  }

  private static ByteBuf readPrivateKey(InputStream in) throws KeyException {
    String content;
    try {
      content = readContent(in);
    } catch (IOException e) {
      throw new KeyException("failed to read key input stream", e);
    }
    Matcher m = KEY_PATTERN.matcher(content);
    if (!m.find()) {
      throw new KeyException("could not find a PKCS #8 private key in input stream");
    }
    ByteBuf base64 = Unpooled.copiedBuffer(m.group(1), CharsetUtil.US_ASCII);
    ByteBuf der = Base64.decode(base64);
    base64.release();
    return der;
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
      return out.toString(CharsetUtil.US_ASCII.name());
    } finally {
      safeClose(out);
    }
  }

  private static void safeClose(OutputStream out) {
    try {
      out.close();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to close a stream.", e);
    }
  }

  private CertificateUtils() {}
}
