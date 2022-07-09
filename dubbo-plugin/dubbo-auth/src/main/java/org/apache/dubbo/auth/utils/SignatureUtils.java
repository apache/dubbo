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
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SignatureUtils {
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    public static String sign(String metadata, String key) throws RuntimeException {
        return sign(metadata.getBytes(StandardCharsets.UTF_8), key);
    }

    public static String sign(Object[] parameters, String metadata, String key) throws RuntimeException {
        if (parameters == null) {
            return sign(metadata, key);
        }
        for (int i = 0; i < parameters.length; i++) {
            if (!(parameters[i] instanceof Serializable)) {
                throw new IllegalArgumentException("The parameter [" + i + "] to be signed was not serializable.");
            }
        }
        Object[] includeMetadata = new Object[parameters.length + 1];
        System.arraycopy(parameters, 0, includeMetadata, 0, parameters.length);
        includeMetadata[parameters.length] = metadata;
        byte[] includeMetadataBytes;
        try {
            includeMetadataBytes = toByteArray(includeMetadata);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate HMAC: " + e.getMessage());
        }
        return sign(includeMetadataBytes, key);
    }

    private static String sign(byte[] data, String key) throws RuntimeException {
        Mac mac;
        try {
            mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate HMAC: no such algorithm exception " + HMAC_SHA256_ALGORITHM);
        }
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA256_ALGORITHM);
        try {
            mac.init(signingKey);
        } catch(InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC: invalid key exception");
        }
        byte[] rawHmac;
        try{
            // compute the hmac on input data bytes
            rawHmac = mac.doFinal(data);
        } catch (IllegalStateException e) {
            throw new RuntimeException("Failed to generate HMAC: " + e.getMessage());
        }
        // base64-encode the hmac
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    private static byte[] toByteArray(Object[] parameters) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(parameters);
            out.flush();
            return bos.toByteArray();
        }
    }
}
