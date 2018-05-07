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
package org.apache.dubbo.bootstrap;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.config.ServiceConfig;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;
import com.alibaba.dubbo.rpc.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A bootstrap class to easily start and stop Dubbo via programmatic API.
 * The bootstrap class will be responsible to cleanup the resources during stop.
 */
public class DubboBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(DubboBootstrap.class);

    /**
     * The list of ServiceConfig
     */
    private List<ServiceConfig> serviceConfigList;

    /**
     * Has it already been destroyed or not?
     */
    private final AtomicBoolean destroyed;

    /**
     * The shutdown hook used when Dubbo is running under embedded environment
     */
    private Thread shutdownHook;

    public DubboBootstrap() {
        this.serviceConfigList = new ArrayList<ServiceConfig>();
        this.destroyed = new AtomicBoolean(false);
        this.shutdownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                if (logger.isInfoEnabled()) {
                    logger.info("Run shutdown hook now.");
                }
                destroy();
            }
        }, "DubboShutdownHook");
    }

    /**
     * Register service config to bootstrap, which will be called during {@link DubboBootstrap#stop()}
     * @param serviceConfig the service
     * @return the bootstrap instance
     */
    public DubboBootstrap regsiterServiceConfig(ServiceConfig serviceConfig) {
        serviceConfigList.add(serviceConfig);
        return this;
    }

    public void start() {
        registerShutdownHook();
        for (ServiceConfig serviceConfig: serviceConfigList) {
            serviceConfig.export();
        }
    }

    public void stop() {
        for (ServiceConfig serviceConfig: serviceConfigList) {
            serviceConfig.unexport();
        }
        destroy();
        removeShutdownHook();
    }

    /**
     * Register the shutdown hook
     */
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Remove this shutdown hook
     */
    public void removeShutdownHook() {
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        catch (IllegalStateException ex) {
            // ignore - VM is already shutting down
        }
    }

    /**
     * Destroy all the resources, including registries and protocols.
     */
    private void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        // destroy all the registries
        AbstractRegistryFactory.destroyAll();
        // destroy all the protocols
        ExtensionLoader<Protocol> loader = ExtensionLoader.getExtensionLoader(Protocol.class);
        for (String protocolName : loader.getLoadedExtensions()) {
            try {
                Protocol protocol = loader.getLoadedExtension(protocolName);
                if (protocol != null) {
                    protocol.destroy();
                }
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
    }
}
