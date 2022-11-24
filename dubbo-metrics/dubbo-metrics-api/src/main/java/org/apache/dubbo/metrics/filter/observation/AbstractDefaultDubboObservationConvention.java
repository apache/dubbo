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
import io.micrometer.common.docs.KeyName;
import io.micrometer.common.lang.Nullable;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContextAttachment;

import static org.apache.dubbo.metrics.filter.observation.DubboObservation.LowCardinalityKeyNames.NET_PEER_NAME;
import static org.apache.dubbo.metrics.filter.observation.DubboObservation.LowCardinalityKeyNames.NET_PEER_PORT;
import static org.apache.dubbo.metrics.filter.observation.DubboObservation.LowCardinalityKeyNames.RPC_METHOD;
import static org.apache.dubbo.metrics.filter.observation.DubboObservation.LowCardinalityKeyNames.RPC_SERVICE;
import static org.apache.dubbo.metrics.filter.observation.DubboObservation.LowCardinalityKeyNames.RPC_SYSTEM;

class AbstractDefaultDubboObservationConvention {
    KeyValues getLowCardinalityKeyValues(Invocation invocation, RpcContextAttachment rpcContextAttachment) {
        KeyValues keyValues = KeyValues.of(RPC_SYSTEM.withValue("apache_dubbo"));
        String serviceName = StringUtils.hasText(invocation.getServiceName()) ? invocation.getServiceName() : readServiceName(invocation.getTargetServiceUniqueName());
        keyValues = appendNonNull(keyValues, RPC_SERVICE, serviceName);
        keyValues = appendNonNull(keyValues, RPC_METHOD, invocation.getMethodName());
        keyValues = appendNonNull(keyValues, NET_PEER_NAME, rpcContextAttachment.getRemoteHostName());
        if (rpcContextAttachment.getRemotePort() == 0) {
            return keyValues;
        }
        int port = rpcContextAttachment.getRemotePort() != 0 ? rpcContextAttachment.getRemotePort() : rpcContextAttachment.getLocalPort();
        return appendNonNull(keyValues, NET_PEER_PORT, String.valueOf(port));
    }

    private KeyValues appendNonNull(KeyValues keyValues, KeyName keyName, @Nullable String value) {
        if (value != null) {
            return keyValues.and(keyName.withValue(value));
        }
        return keyValues;
    }

    String getContextualName(Invocation invocation, RpcContextAttachment rpcContextAttachment) {
        String serviceName = StringUtils.hasText(invocation.getServiceName()) ? invocation.getServiceName() : readServiceName(invocation.getTargetServiceUniqueName());
        String method = StringUtils.hasText(rpcContextAttachment.getMethodName()) ? rpcContextAttachment.getMethodName() : invocation.getMethodName();
        return serviceName + "/" + method;
    }

    private String readServiceName(String targetServiceUniqueName) {
        String[] splitByHyphen = targetServiceUniqueName.split("/"); // foo-provider/a.b.c:1.0.0 or a.b.c:1.0.0
        String withVersion = splitByHyphen.length == 1 ? targetServiceUniqueName : splitByHyphen[1];
        String[] splitByVersion = withVersion.split(":"); // a.b.c:1.0.0
        if (splitByVersion.length == 1) {
            return withVersion;
        }
        return splitByVersion[0]; // a.b.c
    }
}
