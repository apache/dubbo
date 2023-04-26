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
package org.apache.dubbo.metrics.observation;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcContextAttachment;

import io.micrometer.common.KeyValues;

import java.util.List;

import static org.apache.dubbo.metrics.observation.DubboObservationDocumentation.LowCardinalityKeyNames.NET_PEER_NAME;
import static org.apache.dubbo.metrics.observation.DubboObservationDocumentation.LowCardinalityKeyNames.NET_PEER_PORT;

/**
 * Default implementation of the {@link DubboClientObservationConvention}.
 */
public class DefaultDubboClientObservationConvention extends AbstractDefaultDubboObservationConvention implements DubboClientObservationConvention {
    /**
     * Singleton instance of {@link DefaultDubboClientObservationConvention}.
     */
    private static final DubboClientObservationConvention INSTANCE = new DefaultDubboClientObservationConvention();

    public static DubboClientObservationConvention getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return "rpc.client.duration";
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(DubboClientContext context) {
        KeyValues keyValues = super.getLowCardinalityKeyValues(context.getInvocation());
        return withRemoteHostPort(keyValues, context);
    }

    private KeyValues withRemoteHostPort(KeyValues keyValues, DubboClientContext context) {
        List<Invoker<?>> invokedInvokers = context.getInvocation().getInvokedInvokers();
        if (invokedInvokers.isEmpty()) {
            return keyValues;
        }
        // We'll attach tags only from the first invoker
        Invoker<?> invoker = invokedInvokers.get(0);
        URL url = invoker.getUrl();
        RpcContextAttachment rpcContextAttachment = RpcContext.getClientAttachment();
        String remoteHost = remoteHost(rpcContextAttachment, url);
        int remotePort = remotePort(rpcContextAttachment, url);
        return withRemoteHostPort(keyValues, remoteHost, remotePort);
    }

    private String remoteHost(RpcContextAttachment rpcContextAttachment, URL url) {
        String remoteHost = url != null ? url.getHost() : null;
        return remoteHost != null ? remoteHost : rpcContextAttachment.getRemoteHost();
    }

    private int remotePort(RpcContextAttachment rpcContextAttachment, URL url) {
        Integer remotePort = url != null ? url.getPort() : null;
        if (remotePort != null) {
            return remotePort;
        }
        return rpcContextAttachment.getRemotePort() != 0 ? rpcContextAttachment.getRemotePort() : rpcContextAttachment.getLocalPort();
    }

    private KeyValues withRemoteHostPort(KeyValues keyValues, String remoteHostName, int remotePort) {
        keyValues = appendNonNull(keyValues, NET_PEER_NAME, remoteHostName);
        if (remotePort == 0) {
            return keyValues;
        }
        return appendNonNull(keyValues, NET_PEER_PORT, String.valueOf(remotePort));
    }

    @Override
    public String getContextualName(DubboClientContext context) {
        return super.getContextualName(context.getInvocation());
    }
}
