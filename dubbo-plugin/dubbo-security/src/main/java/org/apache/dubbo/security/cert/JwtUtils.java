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

import org.apache.dubbo.common.utils.StringUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

public class JwtUtils {
    public static Endpoint decodeEndpointFromJwt(String jwt, String commonName, List<String> trustedKeys, InetSocketAddress socketAddress) {
        for (String trustedKey : trustedKeys) {
            try {
                ECPublicKey publicKey = getPublicKey(trustedKey);
                DecodedJWT decodedJWT = JWT.require(Algorithm.ECDSA256(publicKey))
                    .build()
                    .verify(jwt);
                if (StringUtils.isNotEmpty(commonName) && !decodedJWT.getClaims().get("cn").asString().equals(commonName)) {
                    continue;
                }
                String spiffeUrl = decodedJWT.getClaims().get("sub").asString();
                String extensions = decodedJWT.getClaims().get("ext").asString();
                return new Endpoint(spiffeUrl, extensions, socketAddress);
            } catch (Exception e) {
                // ignore
            }
        }
        return null;
    }

    private static ECPublicKey getPublicKey(String key) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        PemReader pemReader = new PemReader(new InputStreamReader(new ByteArrayInputStream(key.getBytes())));
        PemObject pemObject = pemReader.readPemObject();
        byte[] content = pemObject.getContent();
        X509EncodedKeySpec spec = new X509EncodedKeySpec(content);
        KeyFactory kf = KeyFactory.getInstance("EC");
        return (ECPublicKey) kf.generatePublic(spec);
    }
}
