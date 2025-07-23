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
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.LRU2Cache;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import io.netty.handler.codec.DateFormatter;
import io.netty.handler.codec.http2.DefaultHttp2Headers;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_PARSE;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_UNSUPPORTED;

public final class StreamUtils {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(StreamUtils.class);

    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();

    private static final int MAX_LRU_HEADER_MAP_SIZE = 10000;

    private static final Map<String, String> lruHeaderMap = new LRU2Cache<>(MAX_LRU_HEADER_MAP_SIZE);

    private StreamUtils() {}

    public static String encodeBase64ASCII(byte[] in) {
        return new String(encodeBase64(in), StandardCharsets.US_ASCII);
    }

    public static byte[] encodeBase64(byte[] in) {
        return BASE64_ENCODER.encode(in);
    }

    public static byte[] decodeASCIIByte(String value) {
        return BASE64_DECODER.decode(value.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Parse and put attachments into headers.
     * Ignore Http2 PseudoHeaderName and internal name.
     * Only strings, dates, and byte arrays are allowed.
     *
     * @param headers              the headers
     * @param attachments          the attachments
     * @param needConvertHeaderKey whether need to convert the header key to lower-case
     */
    public static void putHeaders(
            DefaultHttp2Headers headers, Map<String, Object> attachments, boolean needConvertHeaderKey) {
        putHeaders(attachments, needConvertHeaderKey, headers::set);
    }

    /**
     * Parse and put attachments into headers.
     * Ignore Http2 PseudoHeaderName and internal name.
     * Only strings, dates, and byte arrays are allowed.
     *
     * @param headers              the headers
     * @param attachments          the attachments
     * @param needConvertHeaderKey whether need to convert the header key to lower-case
     */
    public static void putHeaders(HttpHeaders headers, Map<String, Object> attachments, boolean needConvertHeaderKey) {
        putHeaders(attachments, needConvertHeaderKey, headers::set);
    }

    private static void putHeaders(
            Map<String, Object> attachments, boolean needConvertHeaderKey, BiConsumer<CharSequence, String> consumer) {
        if (CollectionUtils.isEmptyMap(attachments)) {
            return;
        }
        Map<String, String> needConvertKeys = new HashMap<>();
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            String key = entry.getKey();
            String lowerCaseKey = lruHeaderMap.computeIfAbsent(key, k -> k.toLowerCase(Locale.ROOT));
            if (TripleHeaderEnum.containsExcludeAttachments(lowerCaseKey)) {
                continue;
            }
            if (needConvertHeaderKey && !lowerCaseKey.equals(key)) {
                needConvertKeys.put(lowerCaseKey, key);
            }
            putHeader(consumer, lowerCaseKey, value);
        }
        if (needConvertKeys.isEmpty()) {
            return;
        }
        consumer.accept(
                TripleHeaderEnum.TRI_HEADER_CONVERT.getKey(),
                TriRpcStatus.encodeMessage(JsonUtils.toJson(needConvertKeys)));
    }

    /**
     * Put a KV pairs into headers.
     *
     * @param consumer outbound headers consumer
     * @param key      the key of the attachment
     * @param value    the value of the attachment (Only strings, dates, and byte arrays are allowed in the attachment value.)
     */
    private static void putHeader(BiConsumer<CharSequence, String> consumer, String key, Object value) {
        try {
            if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
                String str = value.toString();
                consumer.accept(key, str);
            } else if (value instanceof Date) {
                consumer.accept(key, DateFormatter.format((Date) value));
            } else if (value instanceof byte[]) {
                String str = encodeBase64ASCII((byte[]) value);
                consumer.accept(key + TripleConstants.HEADER_BIN_SUFFIX, str);
            } else {
                LOGGER.warn(
                        PROTOCOL_UNSUPPORTED,
                        "",
                        "",
                        "Unsupported attachment k: " + key + " class: "
                                + value.getClass().getName());
            }
        } catch (Throwable t) {
            LOGGER.warn(
                    PROTOCOL_UNSUPPORTED,
                    "",
                    "",
                    "Meet exception when convert single attachment key:" + key + " value=" + value,
                    t);
        }
    }

    /**
     * Convert the given map to attachments. Ignore Http2 PseudoHeaderName and internal name.
     *
     * @param map The map
     * @return the attachments
     */
    public static Map<String, Object> toAttachments(Map<String, Object> map) {
        if (CollectionUtils.isEmptyMap(map)) {
            return Collections.emptyMap();
        }
        Map<String, Object> res = CollectionUtils.newHashMap(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (TripleHeaderEnum.containsExcludeAttachments(key)) {
                continue;
            }
            res.put(key, entry.getValue());
        }
        return res;
    }

    /**
     * Parse and convert headers to attachments. Ignore Http2 PseudoHeaderName and internal name.
     *
     * @param headers the headers
     * @return the attachments
     */
    public static Map<String, Object> toAttachments(HttpHeaders headers) {
        if (headers == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> attachments = CollectionUtils.newHashMap(headers.size());
        for (Map.Entry<CharSequence, String> entry : headers) {
            String key = entry.getKey().toString();
            String value = entry.getValue();
            int len = key.length() - TripleConstants.HEADER_BIN_SUFFIX.length();
            if (len > 0 && TripleConstants.HEADER_BIN_SUFFIX.equals(key.substring(len))) {
                try {
                    putAttachment(attachments, key.substring(0, len), value == null ? null : decodeASCIIByte(value));
                } catch (Exception e) {
                    LOGGER.error(PROTOCOL_FAILED_PARSE, "", "", "Failed to parse response attachment key=" + key, e);
                }
            } else {
                putAttachment(attachments, key, value);
            }
        }

        // try converting upper key
        String converted = headers.getFirst(TripleHeaderEnum.TRI_HEADER_CONVERT.getKey());
        if (converted == null) {
            return attachments;
        }
        String json = TriRpcStatus.decodeMessage(converted);
        Map<String, String> map = JsonUtils.toJavaObject(json, Map.class);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = attachments.remove(key);
            if (value != null) {
                putAttachment(attachments, entry.getValue(), value);
            }
        }
        return attachments;
    }

    /**
     * Put a KV pairs into attachments.
     *
     * @param attachments the map to which the attachment will be added
     * @param key         the key of the header
     * @param value       the value of the header
     */
    private static void putAttachment(Map<String, Object> attachments, String key, Object value) {
        if (TripleHeaderEnum.containsExcludeAttachments(key)) {
            return;
        }
        attachments.put(key, value);
    }
}
