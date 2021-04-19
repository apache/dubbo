package org.apache.dubbo.registry.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

@SPI
public interface XdsCertificateSigner {

    KeyPair request(URL url);

    class KeyPair {
        private final String privateKey;
        private final String publicKey;

        public KeyPair(String privateKey, String publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }
    }
}
