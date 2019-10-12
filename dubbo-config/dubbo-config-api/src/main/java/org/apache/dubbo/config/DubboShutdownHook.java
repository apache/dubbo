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
package org.apache.dubbo.config;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;
import org.apache.dubbo.rpc.GraceFulShutDown;
import org.apache.dubbo.rpc.Protocol;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The shutdown hook thread to do the clean up stuff.
 * This is a singleton in order to ensure there is only one shutdown hook registered.
 * Because {@link ApplicationShutdownHooks} use {@link java.util.IdentityHashMap}
 * to store the shutdown hooks.
 */
public class DubboShutdownHook extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(DubboShutdownHook.class);

    private static final DubboShutdownHook DUBBO_SHUTDOWN_HOOK = new DubboShutdownHook("DubboShutdownHook");
    /**
     * Has it already been registered or not?
     */
    private final AtomicBoolean registered = new AtomicBoolean(false);
    /**
     * Has it already been destroyed or not?
     */
    private final AtomicBoolean destroyed= new AtomicBoolean(false);

    private DubboShutdownHook(String name) {
        super(name);
    }

    public static DubboShutdownHook getDubboShutdownHook() {
        return DUBBO_SHUTDOWN_HOOK;
    }

    @Override
    public void run() {
        if (logger.isInfoEnabled()) {
            logger.info("Run shutdown hook now.");
        }
        doDestroy();
    }

    /**
     * Register the ShutdownHook
     */
    public void register() {
        if (!registered.get() && registered.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(getDubboShutdownHook());
        }
    }

    /**
     * Unregister the ShutdownHook
     */
    public void unregister() {
        if (registered.get() && registered.compareAndSet(true, false)) {
            Runtime.getRuntime().removeShutdownHook(getDubboShutdownHook());
        }
    }

    /**
     * Destroy all the resources, including registries and protocols.
     */
    public void doDestroy() {

        destroyRegistries();
        ExtensionLoader<GraceFulShutDown> loader = ExtensionLoader.getExtensionLoader(GraceFulShutDown.class);
        extensionLoop(loader,GraceFulShutDown::afterRegistriesDestroyed);
        // destroy all the protocols
        destroyProtocols();
        extensionLoop(loader,GraceFulShutDown::afterProtocolDestroyed);
    }

    private void extensionLoop(ExtensionLoader<GraceFulShutDown> loader, Consumer<GraceFulShutDown> consumer){

        for (String extensionName : loader.getLoadedExtensions()){
            try {
                GraceFulShutDown shutDown = loader.getLoadedExtension(extensionName);
                if (null != shutDown){
                    consumer.accept(shutDown);
                }
            }catch (Throwable t){
                logger.warn(t.getMessage(),t);
            }
        }
    }

    public void destroyRegistries(){
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }
        // destroy all the registries
        AbstractRegistryFactory.destroyAll();
    }

    /**
     * Destroy all the protocols.
     */
    public void destroyProtocols() {
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
