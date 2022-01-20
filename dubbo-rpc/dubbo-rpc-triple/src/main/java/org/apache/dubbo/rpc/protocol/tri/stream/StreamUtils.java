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

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.protocol.tri.H2TransportObserver;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;

import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.util.Locale;
import java.util.Map;

public class StreamUtils {
    protected static final Logger LOGGER = LoggerFactory.getLogger(StreamUtils.class);

    /**
     * Parse and put the KV pairs into metadata. Ignore Http2 PseudoHeaderName and internal name.
     * Only raw byte array or string value will be put.
     *
     * @param metadata    the metadata holder
     * @param attachments KV pairs
     */
    public static void convertAttachment(DefaultHttp2Headers headers, Map<String, Object> attachments) {
        if (attachments == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : attachments.entrySet()) {
            final String key = entry.getKey().toLowerCase(Locale.ROOT);
            if (Http2Headers.PseudoHeaderName.isPseudoHeader(key)) {
                continue;
            }
            if (TripleHeaderEnum.containsExcludeAttachments(key)) {
                continue;
            }
            final Object v = entry.getValue();
            convertSingleAttachment(headers, key, v);
        }
    }
    /**
     * Convert each user's attach value to metadata
     *
     * @param metadata {@link Metadata}
     * @param key      metadata key
     * @param v        metadata value (Metadata Only string and byte arrays are allowed)
     */
    private static void convertSingleAttachment(DefaultHttp2Headers headers, String key, Object v) {
        try {
            // todo Support boolean/ numbers
            if (v instanceof String) {
                String str = (String) v;
                headers.set(key, str);
            } else if (v instanceof byte[]) {
                String str = H2TransportObserver.encodeBase64ASCII((byte[]) v);
                headers.set(key + TripleConstant.GRPC_BIN_SUFFIX, str);
            }
        } catch (Throwable t) {
            LOGGER.warn("Meet exception when convert single attachment key:" + key + " value=" + v, t);
        }
    }


}
