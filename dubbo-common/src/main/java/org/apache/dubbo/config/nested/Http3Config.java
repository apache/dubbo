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

import org.apache.dubbo.config.support.Parameter;

import java.io.Serializable;

public class Http3Config implements Serializable {

    private static final long serialVersionUID = -4443828713331129834L;

    public static final int DEFAULT_INITIAL_MAX_DATA = 8_388_608;
    public static final int DEFAULT_INITIAL_MAX_STREAM_DATA_BIDI_LOCAL = 1_048_576;
    public static final int DEFAULT_INITIAL_MAX_STREAM_DATA_BIDI_REMOTE = 1_048_576;
    public static final int DEFAULT_INITIAL_MAX_STREAM_DATA_UNI = 1_048_576;
    public static final long DEFAULT_INITIAL_MAX_STREAMS_BIDI = 1_073_741_824;
    public static final long DEFAULT_INITIAL_MAX_STREAMS_UNI = 1_073_741_824;

    /**
     * Whether to enable HTTP/3 support
     * <p>The default value is false.
     */
    private Boolean enabled;

    /**
     * Whether to enable HTTP/3 negotiation
     * If set to false, HTTP/2 alt-svc negotiation will be skipped, enabling HTTP/3 but disabling HTTP/2 on the consumer side.
     * <p>The default value is true.
     */
    private Boolean negotiation;

    /**
     * See <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_data">set_initial_max_data</a>.
     * <p>The default value is 8MiB.
     */
    private Integer initialMaxData;

    /**
     * If configured this will enable <a href="https://tools.ietf.org/html/draft-ietf-quic-datagram-01">Datagram support.</a>
     */
    private Integer recvQueueLen;

    /**
     * If configured this will enable <a href="https://tools.ietf.org/html/draft-ietf-quic-datagram-01">Datagram support.</a>
     */
    private Integer sendQueueLen;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_stream_data_bidi_local">set_initial_max_stream_data_bidi_local</a>.
     * <p>The default value is 1MiB.
     */
    private Integer initialMaxStreamDataBidiLocal;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_stream_data_bidi_remote">set_initial_max_stream_data_bidi_remote</a>.
     * <p>The default value is 1MiB.
     */
    private Integer initialMaxStreamDataBidiRemote;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_stream_data_uni">set_initial_max_stream_data_uni</a>.
     * <p>The default value is 0.
     */
    private Integer initialMaxStreamDataUni;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_streams_bidi">set_initial_max_streams_bidi</a>.
     * <p>The default value is 1B(2^30).
     */
    private Long initialMaxStreamsBidi;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_initial_max_streams_uni">set_initial_max_streams_uni</a>.
     * <p>
     * <p>The default value is 1B(2^30).
     */
    private Long initialMaxStreamsUni;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_ack_delay_exponent">set_ack_delay_exponent</a>.
     * <p>The default value is 3.
     */
    private Integer maxAckDelayExponent;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_max_ack_delay">set_max_ack_delay</a>.
     * <p>The default value is 25 milliseconds.
     */
    private Integer maxAckDelay;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.set_disable_active_migration">set_disable_active_migration</a>.
     * <p>The default value is {@code false}.
     */
    private Boolean disableActiveMigration;

    /**
     * See
     * <a href="https://docs.rs/quiche/0.6.0/quiche/struct.Config.html#method.enable_hystart">enable_hystart</a>.
     * <p>The default value is {@code true}.
     */
    private Boolean enableHystart;

    /**
     * Sets the congestion control algorithm to use.
     * <p>Supported algorithms are {@code "RENO"} or {@code "CUBIC"} or {@code "BBR"}.
     * <p>The default value is {@code "CUBIC"}.
     */
    private String ccAlgorithm;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getNegotiation() {
        return negotiation;
    }

    public void setNegotiation(Boolean negotiation) {
        this.negotiation = negotiation;
    }

    public Integer getInitialMaxData() {
        return initialMaxData;
    }

    @Parameter(excluded = true)
    public int getInitialMaxDataOrDefault() {
        return initialMaxData == null ? DEFAULT_INITIAL_MAX_DATA : initialMaxData;
    }

    public void setInitialMaxData(Integer initialMaxData) {
        this.initialMaxData = initialMaxData;
    }

    public Integer getRecvQueueLen() {
        return recvQueueLen;
    }

    public void setRecvQueueLen(Integer recvQueueLen) {
        this.recvQueueLen = recvQueueLen;
    }

    public Integer getSendQueueLen() {
        return sendQueueLen;
    }

    public void setSendQueueLen(Integer sendQueueLen) {
        this.sendQueueLen = sendQueueLen;
    }

    public Integer getInitialMaxStreamDataBidiLocal() {
        return initialMaxStreamDataBidiLocal;
    }

    @Parameter(excluded = true)
    public int getInitialMaxStreamDataBidiLocalOrDefault() {
        return initialMaxStreamDataBidiLocal == null
                ? DEFAULT_INITIAL_MAX_STREAM_DATA_BIDI_LOCAL
                : initialMaxStreamDataBidiLocal;
    }

    public void setInitialMaxStreamDataBidiLocal(Integer initialMaxStreamDataBidiLocal) {
        this.initialMaxStreamDataBidiLocal = initialMaxStreamDataBidiLocal;
    }

    public Integer getInitialMaxStreamDataBidiRemote() {
        return initialMaxStreamDataBidiRemote;
    }

    @Parameter(excluded = true)
    public int getInitialMaxStreamDataBidiRemoteOrDefault() {
        return initialMaxStreamDataBidiRemote == null
                ? DEFAULT_INITIAL_MAX_STREAM_DATA_BIDI_REMOTE
                : initialMaxStreamDataBidiRemote;
    }

    public void setInitialMaxStreamDataBidiRemote(Integer initialMaxStreamDataBidiRemote) {
        this.initialMaxStreamDataBidiRemote = initialMaxStreamDataBidiRemote;
    }

    public Integer getInitialMaxStreamDataUni() {
        return initialMaxStreamDataUni;
    }

    @Parameter(excluded = true)
    public int getInitialMaxStreamDataUniOrDefault() {
        return initialMaxStreamDataUni == null ? DEFAULT_INITIAL_MAX_STREAM_DATA_UNI : initialMaxStreamDataUni;
    }

    public void setInitialMaxStreamDataUni(Integer initialMaxStreamDataUni) {
        this.initialMaxStreamDataUni = initialMaxStreamDataUni;
    }

    public Long getInitialMaxStreamsBidi() {
        return initialMaxStreamsBidi;
    }

    @Parameter(excluded = true)
    public long getInitialMaxStreamsBidiOrDefault() {
        return initialMaxStreamsBidi == null ? DEFAULT_INITIAL_MAX_STREAMS_BIDI : initialMaxStreamsBidi;
    }

    public void setInitialMaxStreamsBidi(Long initialMaxStreamsBidi) {
        this.initialMaxStreamsBidi = initialMaxStreamsBidi;
    }

    public Long getInitialMaxStreamsUni() {
        return initialMaxStreamsUni;
    }

    @Parameter(excluded = true)
    public long getInitialMaxStreamsUniOrDefault() {
        return initialMaxStreamsUni == null ? DEFAULT_INITIAL_MAX_STREAMS_UNI : initialMaxStreamsUni;
    }

    public void setInitialMaxStreamsUni(Long initialMaxStreamsUni) {
        this.initialMaxStreamsUni = initialMaxStreamsUni;
    }

    public Integer getMaxAckDelayExponent() {
        return maxAckDelayExponent;
    }

    public void setMaxAckDelayExponent(Integer maxAckDelayExponent) {
        this.maxAckDelayExponent = maxAckDelayExponent;
    }

    public Integer getMaxAckDelay() {
        return maxAckDelay;
    }

    public void setMaxAckDelay(Integer maxAckDelay) {
        this.maxAckDelay = maxAckDelay;
    }

    public Boolean getDisableActiveMigration() {
        return disableActiveMigration;
    }

    public void setDisableActiveMigration(Boolean disableActiveMigration) {
        this.disableActiveMigration = disableActiveMigration;
    }

    public Boolean getEnableHystart() {
        return enableHystart;
    }

    public void setEnableHystart(Boolean enableHystart) {
        this.enableHystart = enableHystart;
    }

    public String getCcAlgorithm() {
        return ccAlgorithm;
    }

    public void setCcAlgorithm(String ccAlgorithm) {
        this.ccAlgorithm = ccAlgorithm;
    }
}
