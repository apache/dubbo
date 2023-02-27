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
import org.apache.dubbo.common.config.Configuration;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import io.grpc.health.v1.DubboHealthTriple;
import io.grpc.health.v1.Health;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.CommonConstants.ANYHOST_VALUE;
import static org.apache.dubbo.rpc.Constants.PROXY_KEY;
import static org.apache.dubbo.rpc.Constants.TRI_BUILTIN_SERVICE_INIT;

/**
 * tri internal service like grpc internal service
 **/
public class TriBuiltinService {

    private ProxyFactory proxyFactory;

    private PathResolver pathResolver;

    private Health healthService;

    private FrameworkModel frameworkModel;

    private ReflectionV1AlphaService reflectionServiceV1Alpha;
    private HealthStatusManager healthStatusManager;
    private Configuration config = ConfigurationUtils.getGlobalConfiguration(
        ApplicationModel.defaultModel());

    private final AtomicBoolean init = new AtomicBoolean();

    public TriBuiltinService(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
        if (enable()) {
            init();
        }
    }

    public void init() {
        if (init.compareAndSet(false, true)) {
            healthStatusManager = new HealthStatusManager(new TriHealthImpl());
            healthService = healthStatusManager.getHealthService();
            reflectionServiceV1Alpha = new ReflectionV1AlphaService();
            proxyFactory = frameworkModel.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
            pathResolver = frameworkModel.getExtensionLoader(PathResolver.class).getDefaultExtension();
            addSingleBuiltinService(DubboHealthTriple.SERVICE_NAME, healthService, Health.class);
            addSingleBuiltinService(ReflectionV1AlphaService.SERVICE_NAME, reflectionServiceV1Alpha,
                ReflectionV1AlphaService.class);
        }
    }

    public boolean enable(){
        return config.getBoolean(TRI_BUILTIN_SERVICE_INIT, false);
    }


    private <T> void addSingleBuiltinService(String serviceName, T impl, Class<T> interfaceClass) {
        ModuleModel internalModule = ApplicationModel.defaultModel().getInternalModule();
        URL url = new ServiceConfigURL(CommonConstants.TRIPLE, null, null, ANYHOST_VALUE, 0,
            serviceName)
            .addParameter(PROXY_KEY, CommonConstants.NATIVE_STUB)
            .setScopeModel(internalModule);
        Invoker<?> invoker = proxyFactory.getInvoker(impl, interfaceClass, url);
        pathResolver.add(serviceName, invoker);
        internalModule.addDestroyListener(scopeModel -> pathResolver.remove(serviceName));
    }

    public HealthStatusManager getHealthStatusManager() {
        return healthStatusManager;
    }
}
