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

import java.util.Map;
import java.util.Objects;

import io.micrometer.observation.transport.SenderContext;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcContextAttachment;

/**
 * Provider context for RPC.
 */
public class DubboClientContext extends SenderContext<Map<String, String>> {

    private final RpcContextAttachment rpcContextAttachment;

    private final Invoker<?> invoker;

    private final Invocation invocation;

    public DubboClientContext(Map<String, String> attachments, RpcContextAttachment rpcContextAttachment, Invoker<?> invoker, Invocation invocation) {
        super((map, key, value) -> Objects.requireNonNull(map).put(key, value));
        this.rpcContextAttachment = rpcContextAttachment;
        this.invoker = invoker;
        this.invocation = invocation;
        setCarrier(attachments);
    }

    public RpcContextAttachment getRpcContextAttachment() {
        return rpcContextAttachment;
    }

    public Invoker<?> getInvoker() {
        return invoker;
    }

    public Invocation getInvocation() {
        return invocation;
    }
}
