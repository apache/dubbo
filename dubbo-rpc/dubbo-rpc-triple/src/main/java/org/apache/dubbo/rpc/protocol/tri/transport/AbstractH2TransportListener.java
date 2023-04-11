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

package org.apache.dubbo.rpc.protocol.tri.transport;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.rpc.TriRpcStatus;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;
import org.apache.dubbo.rpc.protocol.tri.stream.StreamUtils;

import io.netty.handler.codec.http2.Http2Headers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_FAILED_PARSE;

public abstract class AbstractH2TransportListener implements H2TransportListener {

    private static final ErrorTypeAwareLogger LOGGER = LoggerFactory.getErrorTypeAwareLogger(AbstractH2TransportListener.class);

    /**
     * Parse metadata to a KV pairs map.
     *
     * @param trailers the metadata from remote
     * @return KV pairs map
     */
    protected Map<String, Object> headersToMap(Http2Headers trailers, Supplier<Object> convertUpperHeaderSupplier) {
        if (trailers == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> attachments = new HashMap<>(trailers.size());
        for (Map.Entry<CharSequence, CharSequence> header : trailers) {
            String key = header.getKey().toString();
            if (key.endsWith(TripleConstant.HEADER_BIN_SUFFIX)
                && key.length() > TripleConstant.HEADER_BIN_SUFFIX.length()) {
                try {
                    String realKey = key.substring(0,
                        key.length() - TripleConstant.HEADER_BIN_SUFFIX.length());
                    byte[] value = StreamUtils.decodeASCIIByte(header.getValue());
                    attachments.put(realKey, value);
                } catch (Exception e) {
                    LOGGER.error(PROTOCOL_FAILED_PARSE, "", "", "Failed to parse response attachment key=" + key, e);
                }
            } else {
                attachments.put(key, header.getValue().toString());
            }
        }

        // try converting upper key
        Object obj = convertUpperHeaderSupplier.get();
        if (obj == null) {
            return attachments;
        }
        if (obj instanceof String) {
            String json = TriRpcStatus.decodeMessage((String) obj);
            Map<String, String> map = JsonUtils.toJavaObject(json, Map.class);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Object val = attachments.remove(entry.getKey());
                if (val != null) {
                    attachments.put(entry.getValue(), val);
                }
            }
        } else {
            // If convertUpperHeaderSupplier does not return String, just fail...
            // Internal invocation, use INTERNAL_ERROR instead.

            LOGGER.error(INTERNAL_ERROR, "wrong internal invocation", "", "Triple convertNoLowerCaseHeader error, obj is not String");
        }
        return attachments;
    }


    protected Map<String, String> filterReservedHeaders(Http2Headers trailers) {
        if (trailers == null) {
            return Collections.emptyMap();
        }
        Map<String, String> excludeHeaders = new HashMap<>(trailers.size());
        for (Map.Entry<CharSequence, CharSequence> header : trailers) {
            String key = header.getKey().toString();
            if (TripleHeaderEnum.containsExcludeAttachments(key)) {
                excludeHeaders.put(key, trailers.getAndRemove(key).toString());
            }
        }
        return excludeHeaders;
    }

}
