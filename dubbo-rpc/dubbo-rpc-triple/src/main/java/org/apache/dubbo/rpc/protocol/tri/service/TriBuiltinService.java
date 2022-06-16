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
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import io.grpc.health.v1.DubboHealthTriple;
import io.grpc.health.v1.Health;
import io.grpc.reflection.v1.DubboServerReflectionTriple;
import io.grpc.reflection.v1.ServerReflection;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.rpc.Constants.PROXY_KEY;

/**
 * tri internal service like grpc internal service
 **/
public class TriBuiltinService {

    private final ProxyFactory proxyFactory;

    private final PathResolver pathResolver;

    private final Health healthService;

    private final ReflectionService reflectionService;
    private final HealthStatusManager healthStatusManager;

    private final AtomicBoolean init = new AtomicBoolean();

    public TriBuiltinService(FrameworkModel frameworkModel) {
        healthStatusManager = new HealthStatusManager(new TriHealthImpl());
        healthService = healthStatusManager.getHealthService();
        reflectionService = new ReflectionService();
        proxyFactory = frameworkModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
        pathResolver = frameworkModel.getExtensionLoader(PathResolver.class).getDefaultExtension();
        init();
    }

    public void init() {
        if (init.compareAndSet(false, true)) {
            URL url = new ServiceConfigURL(CommonConstants.TRIPLE, null, null, ANYHOST_VALUE, 0,
                DubboHealthTriple.SERVICE_NAME)
                .addParameter(PROXY_KEY, CommonConstants.NATIVE_STUB)
                .setScopeModel(ApplicationModel.defaultModel().getInternalModule());
            Invoker<?> invoker = proxyFactory.getInvoker(healthService, Health.class, url);
            pathResolver.add(DubboHealthTriple.SERVICE_NAME, invoker);

            URL url2 = new ServiceConfigURL(CommonConstants.TRIPLE, null, null, ANYHOST_VALUE, 0,
                DubboServerReflectionTriple.SERVICE_NAME)
                .addParameter(PROXY_KEY, CommonConstants.NATIVE_STUB)
                .setScopeModel(ApplicationModel.defaultModel().getInternalModule());
            Invoker<?> invoker2 = proxyFactory.getInvoker(reflectionService, ServerReflection.class,
                url2);
            pathResolver.add(DubboServerReflectionTriple.SERVICE_NAME, invoker2);
            ApplicationModel.defaultModel().getInternalModule()
                .addDestroyListener(
                    scopeModel -> pathResolver.remove(DubboHealthTriple.SERVICE_NAME));
        }
    }

    public HealthStatusManager getHealthStatusManager() {
        return healthStatusManager;
    }
}
