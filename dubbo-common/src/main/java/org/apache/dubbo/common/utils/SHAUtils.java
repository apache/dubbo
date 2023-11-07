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

import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_UNEXPECTED_EXCEPTION;

/**
 * SHA util.
 */
public class SHAUtils {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(SHAUtils.class);

    private MessageDigest shaInst;

    public enum ShaTypeEnum {
        SHA1("SHA1"),
        SHA256("SHA-256"),
        SHA384("SHA-384"),
        SHA512("SHA-512");
        String shaType;

        ShaTypeEnum(String shaType) {
            this.shaType = shaType;
        }

        public String getShaType() {
            return shaType;
        }
    }

    public SHAUtils() {
        try {
            shaInst = MessageDigest.getInstance(ShaTypeEnum.SHA256.getShaType());
        } catch (NoSuchAlgorithmException e) {
            logger.error(COMMON_UNEXPECTED_EXCEPTION, "", "", "Failed to obtain " + ShaTypeEnum.SHA256.getShaType(), e);
        }
    }

    public SHAUtils(ShaTypeEnum shaTypeEnum) {
        try {
            shaInst = MessageDigest.getInstance(shaTypeEnum.getShaType());
        } catch (NoSuchAlgorithmException e) {
            logger.error(COMMON_UNEXPECTED_EXCEPTION, "", "", "Failed to obtain " + shaTypeEnum.getShaType(), e);
        }
    }

    /**
     * Sha (Secure Hash Algorithm) encryption
     *
     * @ param input specifies the string
     * @ return Encrypted string
     */
    public String getShaHex(String input) {
        input = (input == null ? "" : input);

        // MessageDigest instance is NOT thread-safe
        byte[] bytes;
        synchronized (shaInst) {
            shaInst.update(input.getBytes(StandardCharsets.UTF_8));
            bytes = shaInst.digest();
        }

        StringBuilder builder = new StringBuilder();
        String temp;
        for (byte aByte : bytes) {
            temp = Integer.toHexString(aByte & 0xFF); // Get unsigned integer hexadecimal string
            if (temp.length() == 1) {
                builder.append("0"); // Ensure that each byte is represented by two characters
            }
            builder.append(temp);
        }

        return builder.toString();
    }

    /**
     * SHA salt encryption: getShaHex(getShaHex(input) + getShaHex(salt))
     *
     * @ param input string
     * Param salt
     * @ return Encrypted string
     */
    public String getShaHexBySalt(String input, String salt) {
        return getShaHex(getShaHex(input) + getShaHex(salt));
    }

}
