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
package org.apache.dubbo.registry.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

@SPI
public interface XdsCertificateSigner {

    @Adaptive(value = "signer")
    CertPair GenerateCert(URL url);

    class CertPair {
        private final String privateKey;
        private final String publicKey;
        private final long createTime;
        private final long expireTime;

        public CertPair(String privateKey, String publicKey, long createTime, long expireTime) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.createTime = createTime;
            this.expireTime = expireTime;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public long getCreateTime() {
            return createTime;
        }

        public boolean isExpire() {
            return System.currentTimeMillis() < expireTime;
        }
    }
}
