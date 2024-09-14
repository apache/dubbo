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
package org.apache.dubbo.config.nested;

import org.apache.dubbo.config.support.Nested;
import org.apache.dubbo.config.support.Parameter;

import java.io.Serializable;

/**
 * Configuration for triple protocol.
 */
public class TripleConfig implements Serializable {

    private static final long serialVersionUID = -3682252713701362155L;

    public static final int DEFAULT_MAX_BODY_SIZE = 8_388_608;
    public static final int DEFAULT_MAX_RESPONSE_BODY_SIZE = 8_388_608;
    public static final int DEFAULT_MAX_CHUNK_SIZE = 8_388_608;
    public static final int DEFAULT_MAX_HEADER_SIZE = 8_192;
    public static final int DEFAULT_MAX_INITIAL_LINE_LENGTH = 4_096;
    public static final int DEFAULT_INITIAL_BUFFER_SIZE = 16_384;
    public static final int DEFAULT_HEADER_TABLE_SIZE = 4_096;
    public static final boolean DEFAULT_ENABLE_PUSH = false;
    public static final int DEFAULT_MAX_CONCURRENT_STREAMS = Integer.MAX_VALUE;
    public static final int DEFAULT_INITIAL_WINDOW_SIZE = 8_388_608;
    public static final int DEFAULT_CONNECTION_INITIAL_WINDOW_SIZE_KEY = 65_536;
    public static final int DEFAULT_MAX_FRAME_SIZE = 8_388_608;
    public static final int DEFAULT_MAX_HEADER_LIST_SIZE = 32_768;

    /**
     * Whether enable verbose mode.
     * When true, the application will produce detailed logging output
     * to help with debugging and monitoring. This is useful for
     * troubleshooting and understanding the application's behavior in detail.
     * <p>The default value is false.
     */
    private Boolean verbose;

    /**
     * Maximum allowed size for HTTP request bodies.
     * Limits the size of request to prevent excessively large request.
     * <p>The default value is 8MiB.
     */
    private Integer maxBodySize;

    /**
     * Maximum allowed size for HTTP response bodies.
     * Limits the size of responses to prevent excessively large response.
     * <p>The default value is 8MiB.
     */
    private Integer maxResponseBodySize;

    /**
     * Set the maximum chunk size.
     * HTTP requests and responses can be quite large,
     * in which case it's better to process the data as a stream of chunks.
     * This sets the limit, in bytes, at which Netty will send a chunk down the pipeline.
     * <p>The default value is 8MiB.
     * <p>For HTTP/1
     */
    private Integer maxChunkSize;

    /**
     * Set the maximum line length of header lines.
     * This limits how much memory Netty will use when parsing HTTP header key-value pairs.
     * You would typically set this to the same value as {@link #setMaxInitialLineLength(Integer)}.
     * <p>The default value is 8KiB.
     * <p>For HTTP/1
     */
    private Integer maxHeaderSize;

    /**
     * Set the maximum length of the first line of the HTTP header.
     * This limits how much memory Netty will use when parsed the initial HTTP header line.
     * You would typically set this to the same value as {@link #setMaxHeaderSize(Integer)}.
     * <p>The default value is 4096.
     * <p>For HTTP/1
     */
    private Integer maxInitialLineLength;

    /**
     * Set the initial size of the temporary buffer used when parsing the lines of the HTTP headers.
     * <p>The default value is 16384 octets.
     * <p>For HTTP/1
     */
    private Integer initialBufferSize;

    /**
     * The header table size.
     * <p>For HTTP/1
     */
    private Integer headerTableSize;

    /**
     * Whether to enable push
     * <p>The default value is false.
     * <p>For HTTP/2
     */
    private Boolean enablePush;

    /**
     * Maximum concurrent streams.
     * <p>For HTTP/2
     */
    private Integer maxConcurrentStreams;

    /**
     * Initial window size.
     * <p>For HTTP/2
     */
    private Integer initialWindowSize;

    /**
     * Connection initial window size.
     * <p>For HTTP/2
     */
    private Integer connectionInitialWindowSize;

    /**
     * Maximum frame size.
     * <p>For HTTP/2
     */
    private Integer maxFrameSize;

    /**
     * Maximum header list size.
     * <p>For HTTP/2
     */
    private Integer maxHeaderListSize;

    @Nested
    private RestConfig rest;

    @Nested
    private Http3Config http3;

    @Nested
    private ServletConfig servlet;

    @Nested
    private WebSocketConfig websocket;

    public Boolean getVerbose() {
        return verbose;
    }

    public void setVerbose(Boolean verbose) {
        this.verbose = verbose;
    }

    public Integer getMaxBodySize() {
        return maxBodySize;
    }

    @Parameter(excluded = true)
    public int getMaxBodySizeOrDefault() {
        return maxBodySize == null ? DEFAULT_MAX_BODY_SIZE : maxBodySize;
    }

    public void setMaxBodySize(Integer maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    public Integer getMaxResponseBodySize() {
        return maxResponseBodySize;
    }

    @Parameter(excluded = true)
    public int getMaxResponseBodySizeOrDefault() {
        return maxResponseBodySize == null ? DEFAULT_MAX_RESPONSE_BODY_SIZE : maxResponseBodySize;
    }

    public void setMaxResponseBodySize(Integer maxResponseBodySize) {
        this.maxResponseBodySize = maxResponseBodySize;
    }

    public Integer getMaxChunkSize() {
        return maxChunkSize;
    }

    @Parameter(excluded = true)
    public int getMaxChunkSizeOrDefault() {
        return maxChunkSize == null ? DEFAULT_MAX_CHUNK_SIZE : maxChunkSize;
    }

    public void setMaxChunkSize(Integer maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public Integer getMaxHeaderSize() {
        return maxHeaderSize;
    }

    @Parameter(excluded = true)
    public int getMaxHeaderSizeOrDefault() {
        return maxHeaderSize == null ? DEFAULT_MAX_HEADER_SIZE : maxHeaderSize;
    }

    public void setMaxHeaderSize(Integer maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public Integer getMaxInitialLineLength() {
        return maxInitialLineLength;
    }

    @Parameter(excluded = true)
    public int getMaxInitialLineLengthOrDefault() {
        return maxInitialLineLength == null ? DEFAULT_MAX_INITIAL_LINE_LENGTH : maxInitialLineLength;
    }

    public void setMaxInitialLineLength(Integer maxInitialLineLength) {
        this.maxInitialLineLength = maxInitialLineLength;
    }

    public Integer getInitialBufferSize() {
        return initialBufferSize;
    }

    @Parameter(excluded = true)
    public int getInitialBufferSizeOrDefault() {
        return initialBufferSize == null ? DEFAULT_INITIAL_BUFFER_SIZE : initialBufferSize;
    }

    public void setInitialBufferSize(Integer initialBufferSize) {
        this.initialBufferSize = initialBufferSize;
    }

    public Integer getHeaderTableSize() {
        return headerTableSize;
    }

    @Parameter(excluded = true)
    public int getHeaderTableSizeOrDefault() {
        return headerTableSize == null ? DEFAULT_HEADER_TABLE_SIZE : headerTableSize;
    }

    public void setHeaderTableSize(Integer headerTableSize) {
        this.headerTableSize = headerTableSize;
    }

    public Boolean getEnablePush() {
        return enablePush;
    }

    @Parameter(excluded = true)
    public boolean getEnablePushOrDefault() {
        return enablePush == null ? DEFAULT_ENABLE_PUSH : enablePush;
    }

    public void setEnablePush(Boolean enablePush) {
        this.enablePush = enablePush;
    }

    public Integer getMaxConcurrentStreams() {
        return maxConcurrentStreams;
    }

    @Parameter(excluded = true)
    public int getMaxConcurrentStreamsOrDefault() {
        return maxConcurrentStreams == null ? DEFAULT_MAX_CONCURRENT_STREAMS : maxConcurrentStreams;
    }

    public void setMaxConcurrentStreams(Integer maxConcurrentStreams) {
        this.maxConcurrentStreams = maxConcurrentStreams;
    }

    public Integer getInitialWindowSize() {
        return initialWindowSize;
    }

    @Parameter(excluded = true)
    public int getInitialWindowSizeOrDefault() {
        return initialWindowSize == null ? DEFAULT_INITIAL_WINDOW_SIZE : initialWindowSize;
    }

    public void setInitialWindowSize(Integer initialWindowSize) {
        this.initialWindowSize = initialWindowSize;
    }

    public Integer getConnectionInitialWindowSize() {
        return connectionInitialWindowSize;
    }

    @Parameter(excluded = true)
    public Integer getConnectionInitialWindowSizeOrDefault() {
        return connectionInitialWindowSize == null
                ? DEFAULT_CONNECTION_INITIAL_WINDOW_SIZE_KEY
                : connectionInitialWindowSize;
    }

    public void setConnectionInitialWindowSize(Integer connectionInitialWindowSize) {
        this.connectionInitialWindowSize = connectionInitialWindowSize;
    }

    public Integer getMaxFrameSize() {
        return maxFrameSize;
    }

    @Parameter(excluded = true)
    public int getMaxFrameSizeOrDefault() {
        return maxFrameSize == null ? DEFAULT_MAX_FRAME_SIZE : maxFrameSize;
    }

    public void setMaxFrameSize(Integer maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    public Integer getMaxHeaderListSize() {
        return maxHeaderListSize;
    }

    @Parameter(excluded = true)
    public int getMaxHeaderListSizeOrDefault() {
        return maxHeaderListSize == null ? DEFAULT_MAX_HEADER_LIST_SIZE : maxHeaderListSize;
    }

    public void setMaxHeaderListSize(Integer maxHeaderListSize) {
        this.maxHeaderListSize = maxHeaderListSize;
    }

    public RestConfig getRest() {
        return rest;
    }

    @Parameter(excluded = true)
    public RestConfig getRestOrDefault() {
        if (rest == null) {
            rest = new RestConfig();
        }
        return rest;
    }

    public void setRest(RestConfig rest) {
        this.rest = rest;
    }

    public Http3Config getHttp3() {
        return http3;
    }

    @Parameter(excluded = true)
    public Http3Config getHttp3OrDefault() {
        if (http3 == null) {
            http3 = new Http3Config();
        }
        return http3;
    }

    public void setHttp3(Http3Config http3) {
        this.http3 = http3;
    }

    public ServletConfig getServlet() {
        return servlet;
    }

    public void setServlet(ServletConfig servlet) {
        this.servlet = servlet;
    }

    public WebSocketConfig getWebsocket() {
        return websocket;
    }

    public void setWebsocket(WebSocketConfig websocket) {
        this.websocket = websocket;
    }
}
