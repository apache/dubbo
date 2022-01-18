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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.serial.SerializingExecutor;
import org.apache.dubbo.rpc.CancellationContext;
import org.apache.dubbo.rpc.protocol.tri.Metadata;
import org.apache.dubbo.rpc.protocol.tri.Stream;
import org.apache.dubbo.rpc.protocol.tri.TripleConstant;
import org.apache.dubbo.rpc.protocol.tri.TripleHeaderEnum;

import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Headers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public abstract class AbstractStream implements org.apache.dubbo.rpc.protocol.tri.stream.Stream {
    final URL url;
    final Executor executor;
    final CancellationContext cancellationContext;

    protected static final Logger LOGGER = LoggerFactory.getLogger(Stream.class);

    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();
    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder().withoutPadding();

    protected AbstractStream(URL url, Executor executor) {
        this.url = url;
        this.executor= new SerializingExecutor(executor);
        this.cancellationContext = new CancellationContext();

    }

    /**
     * Parse and put the KV pairs into metadata. Ignore Http2 PseudoHeaderName and internal name.
     * Only raw byte array or string value will be put.
     *
     * @param metadata    the metadata holder
     * @param attachments KV pairs
     */
    protected void convertAttachment(DefaultHttp2Headers headers, Map<String, Object> attachments) {
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
    private void convertSingleAttachment(DefaultHttp2Headers headers, String key, Object v) {
        try {
            // todo Support boolean/ numbers
            if (v instanceof String) {
                String str = (String) v;
                headers.set(key, str);
            } else if (v instanceof byte[]) {
                String str = encodeBase64ASCII((byte[]) v);
                headers.set(key + TripleConstant.GRPC_BIN_SUFFIX, str);
            }
        } catch (Throwable t) {
            LOGGER.warn("Meet exception when convert single attachment key:" + key + " value=" + v, t);
        }
    }

    protected String encodeBase64ASCII(byte[] in) {
        byte[] bytes = encodeBase64(in);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    protected byte[] encodeBase64(byte[] in) {
        return BASE64_ENCODER.encode(in);
    }

    protected byte[] decodeASCIIByte(CharSequence value) {
        return BASE64_DECODER.decode(value.toString().getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public URL url() {
        return url;
    }
}
