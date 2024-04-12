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
package org.apache.dubbo.config;

import java.io.Serializable;

/**
 * Configuration for triple protocol.
 */
public class TripleConfig extends AbstractConfig implements Serializable {

    private static final long serialVersionUID = -3682252713701362155L;

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
     * <p>The default value is 1MiB.
     */
    private Integer maxChunkSize;

    /**
     * Set the maximum line length of header lines.
     * This limits how much memory Netty will use when parsing HTTP header key-value pairs.
     * You would typically set this to the same value as {@link #setMaxInitialLineLength(Integer)}.
     * <p>The default value is 8KiB.
     */
    private Integer maxHeaderSize;

    /**
     * Set the maximum length of the first line of the HTTP header.
     * This limits how much memory Netty will use when parsed the initial HTTP header line.
     * You would typically set this to the same value as {@link #setMaxHeaderSize(Integer)}.
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

    /**
     * Whether to pass through standard HTTP headers, default is false.
     */
    private Boolean passThroughStandardHttpHeaders;

    public Integer getMaxBodySize() {
        return maxBodySize;
    }

    public void setMaxBodySize(Integer maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public Integer getMaxResponseBodySize() {
        return maxResponseBodySize;
    }

    public void setMaxResponseBodySize(Integer maxResponseBodySize) {
        this.maxResponseBodySize = maxResponseBodySize;
    }

    public Integer getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(Integer maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public Integer getMaxHeaderSize() {
        return maxHeaderSize;
    }

    public void setMaxHeaderSize(Integer maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public Integer getMaxInitialLineLength() {
        return maxInitialLineLength;
    }

    public void setMaxInitialLineLength(Integer maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
    }

    public Integer getInitialBufferSize() {
        return initialBufferSize;
    }

    public void setInitialBufferSize(Integer initialBufferSize) {
        this.initialBufferSize = initialBufferSize;
    }

    public Integer getHeaderTableSize() {
        return headerTableSize;
    }

    public void setHeaderTableSize(Integer headerTableSize) {
        this.headerTableSize = headerTableSize;
    }

    public Boolean getEnablePush() {
        return enablePush;
    }

    public void setEnablePush(Boolean enablePush) {
        this.enablePush = enablePush;
    }

    public Integer getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }

    public void setMaxConcurrentStreams(Integer maxConcurrentStreams) {
        this.maxConcurrentStreams = maxConcurrentStreams;
    }

    public Integer getInitialWindowSize() {
        return initialWindowSize;
    }

    public void setInitialWindowSize(Integer initialWindowSize) {
        this.initialWindowSize = initialWindowSize;
    }

    public Integer getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setMaxFrameSize(Integer maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    public Integer getMaxHeaderListSize() {
        return maxHeaderListSize;
    }

    public void setMaxHeaderListSize(Integer maxHeaderListSize) {
        this.maxHeaderListSize = maxHeaderListSize;
    }

    public Boolean getPassThroughStandardHttpHeaders() {
        return passThroughStandardHttpHeaders;
    }

    public void setPassThroughStandardHttpHeaders(Boolean passThroughStandardHttpHeaders) {
        this.passThroughStandardHttpHeaders = passThroughStandardHttpHeaders;
    }

    @Override
    protected void checkDefault() {
        super.checkDefault();

        if (maxBodySize == null) {
            maxBodySize = 1 << 23; // 8MiB
        }
        if (maxResponseBodySize == null) {
            maxResponseBodySize = 1 << 23; // 8MiB
        }
        if (maxChunkSize == null) {
            maxChunkSize = 1 << 20; // 1MiB
        }
        if (maxHeaderSize == null) {
            maxHeaderSize = 8192;
        }
        if (maxInitialLineLength == null) {
            maxInitialLineLength = 4096;
        }
        if (initialBufferSize == null) {
            initialBufferSize = 128;
        }
        if (headerTableSize == null) {
            headerTableSize = 4096;
        }
        if (enablePush == null) {
            enablePush = false;
        }
        if (maxConcurrentStreams == null) {
            maxConcurrentStreams = Integer.MAX_VALUE;
        }
        if (initialWindowSize == null) {
            initialWindowSize = 1 << 23; // 8MiB
        }
        if (maxFrameSize == null) {
            maxFrameSize = 1 << 23; // 8MiB
        }
        if (maxHeaderListSize == null) {
            maxHeaderListSize = 1 << 15; // 32KiB
        }
    }
}
