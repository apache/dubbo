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

package org.apache.dubbo.rpc.protocol.tri.stream;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.LRU2Cache;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;

import io.netty.handler.codec.http2.DefaultHttp2Headers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_UNSUPPORTED;

public class StreamUtils {

    protected static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(StreamUtils.class);

    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();


    private static final int MAX_LRU_HEADER_MAP_SIZE = 10000;

    private static final Map<String, String> lruHeaderMap = new LRU2Cache<>(MAX_LRU_HEADER_MAP_SIZE);

    public static String encodeBase64ASCII(byte[] in) {
        byte[] bytes = encodeBase64(in);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    public static byte[] encodeBase64(byte[] in) {
        return BASE64_ENCODER.encode(in);
    }

    public static byte[] decodeASCIIByte(CharSequence value) {
        return BASE64_DECODER.decode(value.toString().getBytes(StandardCharsets.US_ASCII));
    }

    public static Map<String, Object> toAttachments(Map<String, Object> origin) {
        if (origin == null || origin.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> res = new HashMap<>(origin.size());
        origin.forEach((k, v) -> {
            if (TripleHeaderEnum.containsExcludeAttachments(k)) {
                return;
            }
            res.put(k, v);
        });
        return res;
    }

    /**
     * Parse and put the KV pairs into metadata. Ignore Http2 PseudoHeaderName and internal name.
     * Only raw byte array or string value will be put.
     *
     * @param headers              the metadata holder
     * @param attachments          KV pairs
     * @param needConvertHeaderKey convert flag
     */
    public static void convertAttachment(DefaultHttp2Headers headers,
                                         Map<String, Object> attachments,
                                         boolean needConvertHeaderKey) {
        if (attachments == null) {
            return;
        }
        Map<String, String> needConvertKey = new HashMap<>();
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            String key = lruHeaderMap.get(entry.getKey());
            if (key == null) {
                final String lowerCaseKey = entry.getKey().toLowerCase(Locale.ROOT);
                lruHeaderMap.put(entry.getKey(), lowerCaseKey);
                key = lowerCaseKey;
            }
            if (TripleHeaderEnum.containsExcludeAttachments(key)) {
                continue;
            }
            if (needConvertHeaderKey && !key.equals(entry.getKey())) {
                needConvertKey.put(key, entry.getKey());
            }
            final Object v = entry.getValue();
            convertSingleAttachment(headers, key, v);
        }
        if (!needConvertKey.isEmpty()) {
            String needConvertJson = JsonUtils.toJson(needConvertKey);
            headers.add(TripleHeaderEnum.TRI_HEADER_CONVERT.getHeader(), TriRpcStatus.encodeMessage(needConvertJson));
        }
    }


    public static void convertAttachment(DefaultHttp2Headers headers,
                                         Map<String, Object> attachments) {
        convertAttachment(headers, attachments, false);
    }

    /**
     * Convert each user's attach value to metadata
     *
     * @param headers outbound headers
     * @param key     metadata key
     * @param v       metadata value (Metadata Only string and byte arrays are allowed)
     */
    private static void convertSingleAttachment(DefaultHttp2Headers headers, String key, Object v) {
        try {
            if (v instanceof String || v instanceof Number || v instanceof Boolean) {
                String str = v.toString();
                headers.set(key, str);
            } else if (v instanceof byte[]) {
                String str = encodeBase64ASCII((byte[]) v);
                headers.set(key + TripleConstant.HEADER_BIN_SUFFIX, str);
            } else {
                LOGGER.warn(PROTOCOL_UNSUPPORTED, "", "", "Unsupported attachment k: " + key + " class: " + v.getClass().getName());
            }
        } catch (Throwable t) {
            LOGGER.warn(PROTOCOL_UNSUPPORTED, "", "", "Meet exception when convert single attachment key:" + key + " value=" + v,
                t);
        }
    }


}
