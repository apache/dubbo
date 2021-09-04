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
package org.apache.dubbo.rpc.protocol.tri.service;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleServiceRepository;
import org.apache.dubbo.rpc.model.ServiceDescriptor;
import org.apache.dubbo.rpc.model.ServiceMetadata;
import org.apache.dubbo.rpc.protocol.tri.PathResolver;

import grpc.health.v1.Health;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;

/**
 * tri internal  service like grpc internal service
 **/
public class TriBuiltinService {

    private static final ProxyFactory PROXY_FACTORY =
        ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    private static final PathResolver PATH_RESOLVER = ExtensionLoader.getExtensionLoader(PathResolver.class)
        .getDefaultExtension();

    private static final ModuleServiceRepository repository = ApplicationModel.defaultModel().getDefaultModule().getServiceRepository();

    private static final Map<Class<?>, Object> TRI_SERVICES = new HashMap<>();

    private static final HealthStatusManager HEALTH_STATUS_MANAGER;

    private static final AtomicBoolean init = new AtomicBoolean();

    static {
        HEALTH_STATUS_MANAGER = new HealthStatusManager(new TriHealthImpl());
        TRI_SERVICES.put(Health.class, HEALTH_STATUS_MANAGER.getHealthService());
    }

    public static void init() {
        if (init.compareAndSet(false, true)) {
            TRI_SERVICES.forEach((clz, impl) -> {
                ServiceDescriptor serviceDescriptor = repository.registerService(clz);
                ServiceMetadata serviceMetadata = new ServiceMetadata();
                serviceMetadata.setServiceType(clz);
                serviceMetadata.setTarget(impl);
                serviceMetadata.setServiceInterfaceName(clz.getName());
                serviceMetadata.generateServiceKey();
                repository.registerProvider(
                    clz.getName(),
                    impl,
                    serviceDescriptor,
                    null,
                    serviceMetadata
                );
                int port = 0;
                URL url = new ServiceConfigURL(CommonConstants.TRIPLE, null,
                    null, ANYHOST_VALUE, port, clz.getName());
                Invoker<?> invoker = PROXY_FACTORY.getInvoker(impl, (Class) clz, url);
                PATH_RESOLVER.add(url.getServiceKey(), invoker);
                PATH_RESOLVER.add(url.getServiceInterface(), invoker);
            });
        }
    }

    public static HealthStatusManager getHealthStatusManager() {
        return HEALTH_STATUS_MANAGER;
    }
}
