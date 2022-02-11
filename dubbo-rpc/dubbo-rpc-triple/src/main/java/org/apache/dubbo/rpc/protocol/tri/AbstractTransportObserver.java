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

package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import io.netty.handler.codec.http2.Http2Headers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractTransportObserver implements H2TransportObserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTransportObserver.class);

    /**
     * Parse metadata to a KV pairs map.
     *
     * @param trailers the metadata from remote
     * @return KV pairs map
     */
    protected Map<String, Object> headersToMap(Http2Headers trailers) {
        if (trailers == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> attachments = new HashMap<>();
        for (Map.Entry<CharSequence, CharSequence> header : trailers) {
            String key = header.getKey().toString();
            if (key.endsWith(TripleConstant.GRPC_BIN_SUFFIX) && key.length() > TripleConstant.GRPC_BIN_SUFFIX.length()) {
                try {
                    String realKey = key.substring(0, key.length() - TripleConstant.GRPC_BIN_SUFFIX.length());
                    byte[] value = H2TransportObserver.decodeASCIIByte(header.getValue());
                    attachments.put(realKey, value);
                } catch (Exception e) {
                    LOGGER.error("Failed to parse response attachment key=" + key, e);
                }
            } else {
                attachments.put(key, header.getValue().toString());
            }
        }
        return attachments;
    }


    protected Map<String, String> filterReservedHeaders(Http2Headers trailers) {
        if (trailers == null) {
            return Collections.emptyMap();
        }
        Map<String, String> excludeHeaders = new HashMap<>();
        for (Map.Entry<CharSequence, CharSequence> header : trailers) {
            String key = header.getKey().toString();
            if (Http2Headers.PseudoHeaderName.isPseudoHeader(key)) {
                excludeHeaders.put(key, trailers.getAndRemove(key).toString());
            }
            if (TripleHeaderEnum.containsExcludeAttachments(key)) {
                excludeHeaders.put(key, trailers.getAndRemove(key).toString());
            }
        }
        return excludeHeaders;
    }

}
