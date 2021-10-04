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
package org.apache.dubbo.auth.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Base64;

public class SignatureUtils {
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    public static String sign(String metadata, String key) throws SecurityException {
        try {
            return sign(metadata.getBytes(), key);
        } catch (Exception e) {
            throw new SecurityException("Failed to generate HMAC : " + e.getMessage(), e);
        }
    }

    public static String sign(Object[] parameters, String metadata, String key) {
        try {
            if (parameters == null) {
                return sign(metadata, key);
            }
            boolean notSerializable = Arrays.stream(parameters).anyMatch(parameter -> !(parameter instanceof Serializable));
            if (notSerializable) {
                throw new IllegalArgumentException("");
            }

            Object[] includeMetadata = new Object[parameters.length + 1];
            System.arraycopy(parameters, 0, includeMetadata, 0, parameters.length);
            includeMetadata[parameters.length] = metadata;
            byte[] bytes = toByteArray(includeMetadata);
            return sign(bytes, key);
        } catch (Exception e) {
            throw new SecurityException("Failed to generate HMAC : " + e.getMessage(), e);
        }
    }

    public static String sign(byte[] data, String key) throws SignatureException {
        String result;
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(),
                    HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data);
            // base64-encode the hmac
            result = Base64.getEncoder().encodeToString(rawHmac);

        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : "
                    + e.getMessage());
        }
        return result;
    }

    static byte[] toByteArray(Object[] parameters) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(parameters);
            out.flush();
            return bos.toByteArray();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
    }

}
