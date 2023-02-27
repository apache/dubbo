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

package org.apache.dubbo.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.UrlUtils;

import java.util.List;
import java.util.function.Consumer;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;

public class ListenerRegistryWrapper implements Registry {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ListenerRegistryWrapper.class);

    private final Registry registry;
    private final List<RegistryServiceListener> listeners;


    public ListenerRegistryWrapper(Registry registry, List<RegistryServiceListener> listeners) {
        this.registry = registry;
        this.listeners = listeners;
    }

    @Override
    public URL getUrl() {
        return registry.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return registry.isAvailable();
    }

    @Override
    public void destroy() {
        registry.destroy();
    }

    @Override
    public void register(URL url) {
        try {
            if (registry != null) {
                registry.register(url);
            }
        } finally {
            if (!UrlUtils.isConsumer(url)) {
                listenerEvent(serviceListener -> serviceListener.onRegister(url, registry));
            }
        }
    }

    @Override
    public void unregister(URL url) {
        try {
            if (registry != null) {
                registry.unregister(url);
            }
        } finally {
            if (!UrlUtils.isConsumer(url)) {
                listenerEvent(serviceListener -> serviceListener.onUnregister(url, registry));
            }
        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        try {
            if (registry != null) {
                registry.subscribe(url, listener);
            }
        } finally {
            listenerEvent(serviceListener -> serviceListener.onSubscribe(url, registry));
        }
    }


    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        try {
            registry.unsubscribe(url, listener);
        } finally {
            listenerEvent(serviceListener -> serviceListener.onUnsubscribe(url, registry));

        }
    }

    @Override
    public boolean isServiceDiscovery() {
        return registry.isServiceDiscovery();
    }

    @Override
    public List<URL> lookup(URL url) {
        return registry.lookup(url);
    }

    public Registry getRegistry() {
        return registry;
    }

    private void listenerEvent(Consumer<RegistryServiceListener> consumer) {
        if (CollectionUtils.isNotEmpty(listeners)) {
            RuntimeException exception = null;
            for (RegistryServiceListener listener : listeners) {
                if (listener != null) {
                    try {
                        consumer.accept(listener);
                    } catch (RuntimeException t) {
                        logger.error(INTERNAL_ERROR, "unknown error in registry module", "", t.getMessage(), t);
                        exception = t;
                    }
                }
            }
            if (exception != null) {
                throw exception;
            }
        }
    }
}
