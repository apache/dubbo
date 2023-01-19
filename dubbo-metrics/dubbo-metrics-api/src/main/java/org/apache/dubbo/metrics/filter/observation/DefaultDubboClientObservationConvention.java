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
package org.apache.dubbo.metrics.filter.observation;

import io.micrometer.common.KeyValues;
import org.apache.dubbo.rpc.RpcContextAttachment;
import org.apache.dubbo.rpc.support.RpcUtils;

import static org.apache.dubbo.metrics.filter.observation.DubboObservation.LowCardinalityKeyNames.NET_PEER_NAME;
import static org.apache.dubbo.metrics.filter.observation.DubboObservation.LowCardinalityKeyNames.NET_PEER_PORT;

/**
 * Default implementation of the {@link DubboClientObservationConvention}.
 */
public class DefaultDubboClientObservationConvention extends AbstractDefaultDubboObservationConvention implements DubboClientObservationConvention {
    /**
     * Singleton instance of {@link DefaultDubboClientObservationConvention}.
     */
    public static final DubboClientObservationConvention INSTANCE = new DefaultDubboClientObservationConvention();

    @Override
    public String getName() {
        return "rpc.client.duration";
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(DubboClientContext context) {
        KeyValues keyValues = super.getLowCardinalityKeyValues(context.getInvocation());
        RpcContextAttachment rpcContextAttachment = context.getRpcContextAttachment();
        keyValues = appendNonNull(keyValues, NET_PEER_NAME, rpcContextAttachment.getRemoteHostName());
        if (rpcContextAttachment.getRemotePort() == 0) {
            return keyValues;
        }
        int port = rpcContextAttachment.getRemotePort() != 0 ? rpcContextAttachment.getRemotePort() : rpcContextAttachment.getLocalPort();
        return appendNonNull(keyValues, NET_PEER_PORT, String.valueOf(port));
    }

    @Override
    public String getContextualName(DubboClientContext context) {
        return super.getContextualName(context.getInvocation());
    }
}
