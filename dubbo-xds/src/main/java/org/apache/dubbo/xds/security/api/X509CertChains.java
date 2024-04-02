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
package org.apache.dubbo.xds.security.api;

import org.apache.dubbo.xds.istio.IstioEnv;
import org.apache.dubbo.xds.security.CertificateConvertor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

public class X509CertChains {

    private final byte[] trustChainBytes;

    private final long createTime;

    private final long expireAt;

    public X509CertChains(List<String> pemTrustChains){

        StringBuilder builder = new StringBuilder();
        for (String str : pemTrustChains) {
            builder.append(str);
        }

        this.trustChainBytes = builder.toString().getBytes(StandardCharsets.UTF_8);
        this.createTime = System.currentTimeMillis();
        this.expireAt = createTime + IstioEnv.getInstance().getTrustTTL();
    }

    public List<X509Certificate> readAsCerts() throws CertificateException, IOException {
        return CertificateConvertor.readPemX509CertificateChains(
                Collections.singletonList(new String(trustChainBytes, StandardCharsets.UTF_8)));
    }

    public byte[] readAsBytes() {
        return trustChainBytes;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public long getCreateTime() {
        return createTime;
    }
}
