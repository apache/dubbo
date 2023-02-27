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

package org.apache.dubbo.rpc.stub;

import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.model.ServiceDescriptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class StubSuppliers {

    private static final Map<String, Function<Invoker<?>, Object>> STUB_SUPPLIERS = new ConcurrentHashMap<>();
    private static final Map<String, ServiceDescriptor> SERVICE_DESCRIPTOR_MAP = new ConcurrentHashMap<>();

    public static void addDescriptor(String interfaceName, ServiceDescriptor serviceDescriptor) {
        SERVICE_DESCRIPTOR_MAP.put(interfaceName, serviceDescriptor);
    }
    public static void addSupplier(String interfaceName, Function<Invoker<?>, Object> supplier) {
        STUB_SUPPLIERS.put(interfaceName, supplier);
    }

    public static <T> T createStub(String interfaceName, Invoker<T> invoker) {
        //TODO DO not hack here
        if (!STUB_SUPPLIERS.containsKey(interfaceName)) {
            ReflectUtils.forName(stubClassName(interfaceName));
            if (!STUB_SUPPLIERS.containsKey(interfaceName)) {
                throw new IllegalStateException(
                    "Can not find any stub supplier for " + interfaceName);
            }
        }
        return (T) STUB_SUPPLIERS.get(interfaceName).apply(invoker);
    }

    private static String stubClassName(String interfaceName) {
        int idx = interfaceName.lastIndexOf('.');
        String pkg = interfaceName.substring(0, idx + 1);
        String name = interfaceName.substring(idx + 1);
        return pkg + "Dubbo" + name + "Triple";
    }

    public static ServiceDescriptor getServiceDescriptor(String interfaceName) {
        //TODO DO not hack here
        if (!SERVICE_DESCRIPTOR_MAP.containsKey(interfaceName)) {
            ReflectUtils.forName(stubClassName(interfaceName));
            if (!SERVICE_DESCRIPTOR_MAP.containsKey(interfaceName)) {
                throw new IllegalStateException(
                    "Can not find any stub supplier for " + interfaceName);
            }
        }
        return SERVICE_DESCRIPTOR_MAP.get(interfaceName);
    }
}
