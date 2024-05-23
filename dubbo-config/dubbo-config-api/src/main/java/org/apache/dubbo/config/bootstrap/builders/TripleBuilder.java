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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.nested.TripleConfig;

/**
 * This is a builder for build {@link TripleConfig}.
 *
 * @since 3.3
 */
public class TripleBuilder {

    /**
     * Maximum allowed size for HTTP1 request bodies.
     * Limits the size of request to prevent excessively large request.
     * <p>The default value is 8MiB.
     */
    private Integer maxBodySize;

    /**
     * Maximum allowed size for HTTP1 response bodies.
     * Limits the size of responses to prevent excessively large response.
     * <p>The default value is 8MiB.
     */
    private Integer maxResponseBodySize;

    /**
     * Set the maximum chunk size.
     * HTTP requests and responses can be quite large, in which case it's better to process the data as a stream of
     * chunks.
     * This sets the limit, in bytes, at which Netty will send a chunk down the pipeline.
     * <p>The default value is 8KiB.
     */
    private Integer maxChunkSize;

    /**
     * Set the maximum line length of header lines.
     * This limits how much memory Netty will use when parsing HTTP header key-value pairs.
     * You would typically set this to the same value as {@link #maxInitialLineLength(Integer)}.
     * <p>The default value is 8KiB.
     */
    private Integer maxHeaderSize;

    /**
     * Set the maximum length of the first line of the HTTP header.
     * This limits how much memory Netty will use when parsed the initial HTTP header line.
     * You would typically set this to the same value as {@link #maxHeaderSize(Integer)}.
     * <p>The default value is 4096.
     */
    private Integer maxInitialLineLength;

    /**
     * Set the initial size of the temporary buffer used when parsing the lines of the HTTP headers.
     * <p>The default value is 128 octets.
     */
    private Integer initialBufferSize;

    /**
     * The header table size.
     */
    private Integer headerTableSize;

    /**
     * Whether to enable push, default is false.
     */
    private Boolean enablePush;

    /**
     * Maximum concurrent streams.
     */
    private Integer maxConcurrentStreams;

    /**
     * Initial window size.
     */
    private Integer initialWindowSize;

    /**
     * Maximum frame size.
     */
    private Integer maxFrameSize;

    /**
     * Maximum header list size.
     */
    private Integer maxHeaderListSize;

    public static TripleBuilder newBuilder() {
        return new TripleBuilder();
    }

    public TripleBuilder maxBodySize(Integer maxBodySize) {
        this.maxBodySize = maxBodySize;
        return getThis();
    }

    public TripleBuilder maxResponseBodySize(Integer maxResponseBodySize) {
        this.maxResponseBodySize = maxResponseBodySize;
        return getThis();
    }

    public TripleBuilder maxChunkSize(Integer maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
        return getThis();
    }

    public TripleBuilder maxHeaderSize(Integer maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
        return getThis();
    }

    public TripleBuilder maxInitialLineLength(Integer maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
        return getThis();
    }

    public TripleBuilder initialBufferSize(Integer initialBufferSize) {
        this.initialBufferSize = initialBufferSize;
        return getThis();
    }

    public TripleBuilder headerTableSize(Integer headerTableSize) {
        this.headerTableSize = headerTableSize;
        return getThis();
    }

    public TripleBuilder enablePush(Boolean enablePush) {
        this.enablePush = enablePush;
        return getThis();
    }

    public TripleBuilder maxConcurrentStreams(Integer maxConcurrentStreams) {
        this.maxConcurrentStreams = maxConcurrentStreams;
        return getThis();
    }

    public TripleBuilder initialWindowSize(Integer initialWindowSize) {
        this.initialWindowSize = initialWindowSize;
        return getThis();
    }

    public TripleBuilder maxFrameSize(Integer maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
        return getThis();
    }

    public TripleBuilder maxHeaderListSize(Integer maxHeaderListSize) {
        this.maxHeaderListSize = maxHeaderListSize;
        return getThis();
    }

    protected TripleBuilder getThis() {
        return this;
    }

    public TripleConfig build() {
        TripleConfig triple = new TripleConfig();

        if (maxBodySize != null) {
            triple.setMaxBodySize(maxBodySize);
        }
        if (maxResponseBodySize != null) {
            triple.setMaxResponseBodySize(maxResponseBodySize);
        }
        if (maxChunkSize != null) {
            triple.setMaxChunkSize(maxChunkSize);
        }
        if (maxHeaderSize != null) {
            triple.setMaxHeaderSize(maxHeaderSize);
        }
        if (maxInitialLineLength != null) {
            triple.setMaxInitialLineLength(maxInitialLineLength);
        }
        if (initialBufferSize != null) {
            triple.setInitialBufferSize(initialBufferSize);
        }
        if (headerTableSize != null) {
            triple.setHeaderTableSize(headerTableSize);
        }
        if (enablePush != null) {
            triple.setEnablePush(enablePush);
        }
        if (maxConcurrentStreams != null) {
            triple.setMaxConcurrentStreams(maxConcurrentStreams);
        }
        if (initialWindowSize != null) {
            triple.setInitialWindowSize(initialWindowSize);
        }
        if (maxFrameSize != null) {
            triple.setMaxFrameSize(maxFrameSize);
        }
        if (maxHeaderListSize != null) {
            triple.setMaxHeaderListSize(maxHeaderListSize);
        }
        return triple;
    }
}
