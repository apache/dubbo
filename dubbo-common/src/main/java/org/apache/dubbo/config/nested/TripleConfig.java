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

import java.io.Serializable;

/**
 * Configuration for triple protocol.
 */
public class TripleConfig implements Serializable {

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
     * <p>The default value is 8MiB.
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
     * <p>The default value is 16384 octets.
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
     * Enable http3 support
     * <p>The default value is false.
     */
    private Boolean enableHttp3;

    /**
     * See <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_data">set_initial_max_data</a>.
     * <p>The default value is 8MiB.
     */
    private Integer http3InitialMaxData;

    /**
     * If configured this will enable <a href="https://tools.ietf.org/html/draft-ietf-quic-datagram-01">Datagram support.</a>
     */
    private Integer http3RecvQueueLen;

    /**
     * If configured this will enable <a href="https://tools.ietf.org/html/draft-ietf-quic-datagram-01">Datagram support.</a>
     */
    private Integer http3SendQueueLen;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_stream_data_bidi_local">set_initial_max_stream_data_bidi_local</a>.
     * <p>The default value is 1MiB.
     */
    private Integer http3InitialMaxStreamDataBidiLocal;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_stream_data_bidi_remote">set_initial_max_stream_data_bidi_remote</a>.
     * <p>The default value is 1MiB.
     */
    private Integer http3InitialMaxStreamDataBidiRemote;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_stream_data_uni">set_initial_max_stream_data_uni</a>.
     * <p>The default value is 0.
     */
    private Integer http3InitialMaxStreamDataUni;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_streams_bidi">set_initial_max_streams_bidi</a>.
     * <p>The default value is 1B(2^30).
     */
    private Long http3InitialMaxStreamsBidi;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_streams_uni">set_initial_max_streams_uni</a>.
     * <p>
     * <p>The default value is 1B(2^30).
     */
    private Long http3InitialMaxStreamsUni;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_ack_delay_exponent">set_ack_delay_exponent</a>.
     * <p>The default value is 3.
     */
    private Integer http3MaxAckDelayExponent;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_max_ack_delay">set_max_ack_delay</a>.
     * <p>The default value is 25 milliseconds.
     */
    private Integer http3MaxAckDelay;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_disable_active_migration">set_disable_active_migration</a>.
     * <p>The default value is {@code false}.
     */
    private Boolean http3DisableActiveMigration;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.enable_hystart">enable_hystart</a>.
     * <p>The default value is {@code true}.
     */
    private Boolean http3EnableHystart;

    /**
     * Sets the congestion control algorithm to use.
     * <p>Supported algorithms are {@code "RENO"} or {@code "CUBIC"} or {@code "BBR"}.
     * <p>The default value is {@code "CUBIC"}.
     */
    private String http3CcAlgorithm;

    /**
     * Enable servlet support, requests are transport through the servlet container,
     * which only supports unary calls due to protocol limitations
     * <p>The default value is false.
     */
    private Boolean enableServlet;

    /**
     * The URL patterns that the servlet filter will be registered for.
     * <p>The default value is '/*'.
     */
    private String[] servletFilterUrlPatterns;

    /**
     * The order of the servlet filter.
     * <p>The default value is -1000000.
     */
    private Integer servletFilterOrder;

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

    public Boolean getEnableHttp3() {
        return enableHttp3;
    }

    public void setEnableHttp3(Boolean enableHttp3) {
        this.enableHttp3 = enableHttp3;
    }

    public Integer getHttp3InitialMaxData() {
        return http3InitialMaxData;
    }

    public void setHttp3InitialMaxData(Integer http3InitialMaxData) {
        this.http3InitialMaxData = http3InitialMaxData;
    }

    public Integer getHttp3RecvQueueLen() {
        return http3RecvQueueLen;
    }

    public void setHttp3RecvQueueLen(Integer http3RecvQueueLen) {
        this.http3RecvQueueLen = http3RecvQueueLen;
    }

    public Integer getHttp3SendQueueLen() {
        return http3SendQueueLen;
    }

    public void setHttp3SendQueueLen(Integer http3SendQueueLen) {
        this.http3SendQueueLen = http3SendQueueLen;
    }

    public Integer getHttp3InitialMaxStreamDataBidiLocal() {
        return http3InitialMaxStreamDataBidiLocal;
    }

    public void setHttp3InitialMaxStreamDataBidiLocal(Integer http3InitialMaxStreamDataBidiLocal) {
        this.http3InitialMaxStreamDataBidiLocal = http3InitialMaxStreamDataBidiLocal;
    }

    public Integer getHttp3InitialMaxStreamDataBidiRemote() {
        return http3InitialMaxStreamDataBidiRemote;
    }

    public void setHttp3InitialMaxStreamDataBidiRemote(Integer http3InitialMaxStreamDataBidiRemote) {
        this.http3InitialMaxStreamDataBidiRemote = http3InitialMaxStreamDataBidiRemote;
    }

    public Integer getHttp3InitialMaxStreamDataUni() {
        return http3InitialMaxStreamDataUni;
    }

    public void setHttp3InitialMaxStreamDataUni(Integer http3InitialMaxStreamDataUni) {
        this.http3InitialMaxStreamDataUni = http3InitialMaxStreamDataUni;
    }

    public Long getHttp3InitialMaxStreamsBidi() {
        return http3InitialMaxStreamsBidi;
    }

    public void setHttp3InitialMaxStreamsBidi(Long http3InitialMaxStreamsBidi) {
        this.http3InitialMaxStreamsBidi = http3InitialMaxStreamsBidi;
    }

    public Long getHttp3InitialMaxStreamsUni() {
        return http3InitialMaxStreamsUni;
    }

    public void setHttp3InitialMaxStreamsUni(Long http3InitialMaxStreamsUni) {
        this.http3InitialMaxStreamsUni = http3InitialMaxStreamsUni;
    }

    public Integer getHttp3MaxAckDelayExponent() {
        return http3MaxAckDelayExponent;
    }

    public void setHttp3MaxAckDelayExponent(Integer http3MaxAckDelayExponent) {
        this.http3MaxAckDelayExponent = http3MaxAckDelayExponent;
    }

    public Integer getHttp3MaxAckDelay() {
        return http3MaxAckDelay;
    }

    public void setHttp3MaxAckDelay(Integer http3MaxAckDelay) {
        this.http3MaxAckDelay = http3MaxAckDelay;
    }

    public Boolean getHttp3DisableActiveMigration() {
        return http3DisableActiveMigration;
    }

    public void setHttp3DisableActiveMigration(Boolean http3DisableActiveMigration) {
        this.http3DisableActiveMigration = http3DisableActiveMigration;
    }

    public Boolean getHttp3EnableHystart() {
        return http3EnableHystart;
    }

    public void setHttp3EnableHystart(Boolean http3EnableHystart) {
        this.http3EnableHystart = http3EnableHystart;
    }

    public String getHttp3CcAlgorithm() {
        return http3CcAlgorithm;
    }

    public void setHttp3CcAlgorithm(String http3CcAlgorithm) {
        this.http3CcAlgorithm = http3CcAlgorithm;
    }

    public Boolean getEnableServlet() {
        return enableServlet;
    }

    public void setEnableServlet(Boolean enableServlet) {
        this.enableServlet = enableServlet;
    }

    public String[] getServletFilterUrlPatterns() {
        return servletFilterUrlPatterns;
    }

    public void setServletFilterUrlPatterns(String[] servletFilterUrlPatterns) {
        this.servletFilterUrlPatterns = servletFilterUrlPatterns;
    }

    public Integer getServletFilterOrder() {
        return servletFilterOrder;
    }

    public void setServletFilterOrder(Integer servletFilterOrder) {
        this.servletFilterOrder = servletFilterOrder;
    }

    public void checkDefault() {
        if (maxBodySize == null) {
            maxBodySize = 1 << 23;
        }
        if (maxResponseBodySize == null) {
            maxResponseBodySize = 1 << 23;
        }
        if (maxChunkSize == null) {
            maxChunkSize = 1 << 23;
        }
        if (maxHeaderSize == null) {
            maxHeaderSize = 8192;
        }
        if (maxInitialLineLength == null) {
            maxInitialLineLength = 4096;
        }
        if (initialBufferSize == null) {
            initialBufferSize = 16384;
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
            initialWindowSize = 1 << 23;
        }
        if (maxFrameSize == null) {
            maxFrameSize = 1 << 23;
        }
        if (maxHeaderListSize == null) {
            maxHeaderListSize = 1 << 15;
        }
    }
}
