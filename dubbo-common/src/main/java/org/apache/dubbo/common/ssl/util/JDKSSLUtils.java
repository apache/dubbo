package org.apache.dubbo.common.ssl.util;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_CLOSE_STREAM;

public class JDKSSLUtils {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(JDKSSLUtils.class);

    public static SSLContext buildJDKSSLContext(InputStream keyCertChainPathStream,
                                                InputStream privateKeyPathStream,
                                                InputStream trustCertStream, String password) {


        try {

            char[] passwordCharArray = password == null ? new char[0] : password.toCharArray();


            SSLContext sslContext = createSslContext();

            // key manage factory
            KeyManagerFactory keyManagerFactory = createKeyManagerFactory(privateKeyPathStream, passwordCharArray);

            //trust manage factory
            TrustManagerFactory trustManagerFactory = createTrustManagerFactory(trustCertStream, passwordCharArray);

            TrustManager[] trustManagers = buildTrustManagers(trustManagerFactory);

            // init ssl context
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, null);

            return sslContext;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not find certificate file or the certificate is invalid.", e);
        } finally {
            JDKSSLUtils.safeCloseStream(trustCertStream);
            JDKSSLUtils.safeCloseStream(keyCertChainPathStream);
            JDKSSLUtils.safeCloseStream(privateKeyPathStream);
        }

    }

    public static SSLContext createSslContext() throws NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        return sslContext;
    }

    public static KeyStore createJDKKeyStore(InputStream privateKeyPathStream, char[] passwordCharArray) throws Exception {
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
        KeyStore trustStore = createJDKKeyStore(trustCertStream, passwordCharArray);
        trustManagerFactory.init(trustStore);
        return trustManagerFactory;
    }

    public static KeyManagerFactory createKeyManagerFactory(InputStream privateKeyPathStream, char[] passwordCharArray) throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        KeyStore keyStore = createJDKKeyStore(privateKeyPathStream, passwordCharArray);
//            keyStore.setKeyEntry(ALIAS, key, passwordCharArray, certChain);
        keyManagerFactory.init(keyStore, passwordCharArray);
        return keyManagerFactory;
    }

    public static void safeCloseStream(InputStream stream) {
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


        return new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }
        };
    }


}
