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

package org.apache.dubbo.common.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 util.
 */
public class Base64Utils {

    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();

    /**
     * Base64 encode, byte[] to String
     *
     * @param bytes byte[]
     * @return String
     */
    public String encodeBytesToString(byte[] bytes) {
        return encoder.encodeToString(bytes);
    }

    /**
     * Base64 decode, String to byte[]
     *
     * @param text String
     * @return byte[]
     */
    public byte[] decodeStringToBytes(String text) {
        return decoder.decode(text);
    }

    /**
     * Base64 encode, String to String
     *
     * @param text String
     * @return Base64 format string
     */
    public String encode(String text) {
        return encoder.encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64 decode, String to String
     *
     * @param base64Text Base64 format string
     * @return String
     */
    public String decode(String base64Text) {
        return new String(decoder.decode(base64Text), StandardCharsets.UTF_8);
    }

}
