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
import org.apache.dubbo.config.nested.TripleConfig;

import io.netty.incubator.codec.quic.QuicCodecBuilder;
import io.netty.incubator.codec.quic.QuicCongestionControlAlgorithm;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

final class Helper {

    @SuppressWarnings("unchecked")
    static <T extends QuicCodecBuilder<T>> T configCodec(QuicCodecBuilder<T> builder, URL url) {
        TripleConfig tripleConfig = ConfigManager.getProtocol(url).getTriple();
        if (tripleConfig.getHttp3InitialMaxData() != null) {
            builder.initialMaxData(tripleConfig.getHttp3InitialMaxData());
        }
        if (tripleConfig.getHttp3RecvQueueLen() != null && tripleConfig.getHttp3SendQueueLen() != null) {
            builder.datagram(tripleConfig.getHttp3RecvQueueLen(), tripleConfig.getHttp3SendQueueLen());
        }
        if (tripleConfig.getHttp3InitialMaxStreamDataBidiLocal() != null) {
            builder.initialMaxStreamDataBidirectionalLocal(tripleConfig.getHttp3InitialMaxStreamDataBidiLocal());
        }
        if (tripleConfig.getHttp3InitialMaxStreamDataBidiRemote() != null) {
            builder.initialMaxStreamDataBidirectionalRemote(tripleConfig.getHttp3InitialMaxStreamDataBidiRemote());
        }
        if (tripleConfig.getHttp3InitialMaxStreamDataUni() != null) {
            builder.initialMaxStreamDataUnidirectional(tripleConfig.getHttp3InitialMaxStreamDataUni());
        }
        if (tripleConfig.getHttp3InitialMaxStreamsBidi() != null) {
            builder.initialMaxStreamsBidirectional(tripleConfig.getHttp3InitialMaxStreamsBidi());
        }
        if (tripleConfig.getHttp3InitialMaxStreamsUni() != null) {
            builder.initialMaxStreamsUnidirectional(tripleConfig.getHttp3InitialMaxStreamsUni());
        }
        if (tripleConfig.getHttp3MaxAckDelayExponent() != null) {
            builder.ackDelayExponent(tripleConfig.getHttp3MaxAckDelayExponent());
        }
        if (tripleConfig.getHttp3MaxAckDelay() != null) {
            builder.maxAckDelay(tripleConfig.getHttp3MaxAckDelay(), MILLISECONDS);
        }
        if (tripleConfig.getHttp3DisableActiveMigration() != null) {
            builder.activeMigration(tripleConfig.getHttp3DisableActiveMigration());
        }
        if (tripleConfig.getHttp3EnableHystart() != null) {
            builder.hystart(tripleConfig.getHttp3EnableHystart());
        }
        if (tripleConfig.getHttp3CcAlgorithm() != null) {
            if ("RENO".equalsIgnoreCase(tripleConfig.getHttp3CcAlgorithm())) {
                builder.congestionControlAlgorithm(QuicCongestionControlAlgorithm.RENO);
            } else if ("BBR".equalsIgnoreCase(tripleConfig.getHttp3CcAlgorithm())) {
                builder.congestionControlAlgorithm(QuicCongestionControlAlgorithm.BBR);
            }
        }
        return (T) builder;
    }
}
