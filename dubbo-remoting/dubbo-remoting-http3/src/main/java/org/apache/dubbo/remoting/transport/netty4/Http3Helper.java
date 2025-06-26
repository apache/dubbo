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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.Http3Config;

import io.netty.incubator.codec.quic.QuicCodecBuilder;
import io.netty.incubator.codec.quic.QuicCongestionControlAlgorithm;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class Http3Helper {

    @SuppressWarnings("unchecked")
    static <T extends QuicCodecBuilder<T>> T configCodec(QuicCodecBuilder<T> builder, URL url) {
        Http3Config config =
                ConfigManager.getProtocolOrDefault(url).getTripleOrDefault().getHttp3OrDefault();
        builder.initialMaxData(config.getInitialMaxDataOrDefault())
                .initialMaxStreamDataBidirectionalLocal(config.getInitialMaxStreamDataBidiLocalOrDefault())
                .initialMaxStreamDataBidirectionalRemote(config.getInitialMaxStreamDataBidiRemoteOrDefault())
                .initialMaxStreamDataUnidirectional(config.getInitialMaxStreamDataUniOrDefault())
                .initialMaxStreamsBidirectional(config.getInitialMaxStreamsBidiOrDefault())
                .initialMaxStreamsUnidirectional(config.getInitialMaxStreamsUniOrDefault());

        if (config.getRecvQueueLen() != null && config.getSendQueueLen() != null) {
            builder.datagram(config.getRecvQueueLen(), config.getSendQueueLen());
        }
        if (config.getMaxAckDelayExponent() != null) {
            builder.ackDelayExponent(config.getMaxAckDelayExponent());
        }
        if (config.getMaxAckDelay() != null) {
            builder.maxAckDelay(config.getMaxAckDelay(), MILLISECONDS);
        }
        if (config.getDisableActiveMigration() != null) {
            builder.activeMigration(config.getDisableActiveMigration());
        }
        if (config.getEnableHystart() != null) {
            builder.hystart(config.getEnableHystart());
        }
        if (config.getCcAlgorithm() != null) {
            if ("RENO".equalsIgnoreCase(config.getCcAlgorithm())) {
                builder.congestionControlAlgorithm(QuicCongestionControlAlgorithm.RENO);
            } else if ("BBR".equalsIgnoreCase(config.getCcAlgorithm())) {
                builder.congestionControlAlgorithm(QuicCongestionControlAlgorithm.BBR);
            }
        }
        return (T) builder;
    }
}
