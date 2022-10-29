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
package org.apache.dubbo.rpc.protocol.grpc;

import io.grpc.BindableService;
import io.grpc.HandlerRegistry;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class DubboHandlerRegistry extends HandlerRegistry {

    private final Map<String, ServerServiceDefinition> services = new ConcurrentHashMap<>();
    private final Map<String, ServerMethodDefinition<?, ?>> methods = new ConcurrentHashMap<>();

    public DubboHandlerRegistry() {
    }

    /**
     * Returns the service definitions in this registry.
     */
    @Override
    public List<ServerServiceDefinition> getServices() {
        return Collections.unmodifiableList(new ArrayList<>(services.values()));
    }

    @Nullable
    @Override
    public ServerMethodDefinition<?, ?> lookupMethod(String methodName, @Nullable String authority) {
        // TODO (carl-mastrangelo): honor authority header.
        return methods.get(methodName);
    }

    void addService(BindableService bindableService, String key) {
        ServerServiceDefinition service = bindableService.bindService();
        services.put(key, service);
        for (ServerMethodDefinition<?, ?> method : service.getMethods()) {
            methods.put(method.getMethodDescriptor().getFullMethodName(), method);
        }
    }

    void removeService(String serviceKey) {
        ServerServiceDefinition service = services.remove(serviceKey);
        if (null != service) {
            for (ServerMethodDefinition<?, ?> method : service.getMethods()) {
                methods.remove(method.getMethodDescriptor().getFullMethodName(), method);
            }
        }
    }
}
