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
package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;

import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

/**
 * AbstractRegistryFactory. (SPI, Singleton, ThreadSafe)
 *
 * @see org.apache.dubbo.registry.RegistryFactory
 */
public abstract class AbstractRegistryFactory implements RegistryFactory, ScopeModelAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRegistryFactory.class);

    private RegistryManager registryManager;
    protected ApplicationModel applicationModel;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
        this.registryManager = applicationModel.getBeanFactory().getBean(RegistryManager.class);
    }

    @Override
    public Registry getRegistry(URL url) {
        if (registryManager == null) {
            throw new IllegalStateException("Unable to fetch RegistryManager from ApplicationModel BeanFactory. " +
                "Please check if `setApplicationModel` has been override.");
        }

        Registry defaultNopRegistry = registryManager.getDefaultNopRegistryIfDestroyed();
        if (null != defaultNopRegistry) {
            return defaultNopRegistry;
        }

        url = URLBuilder.from(url)
            .setPath(RegistryService.class.getName())
            .addParameter(INTERFACE_KEY, RegistryService.class.getName())
            .removeParameter(TIMESTAMP_KEY)
            .removeAttribute(EXPORT_KEY)
            .removeAttribute(REFER_KEY)
            .build();
        String key = createRegistryCacheKey(url);
        Registry registry = null;
        boolean check = url.getParameter(CHECK_KEY, true) && url.getPort() != 0;
        // Lock the registry access process to ensure a single instance of the registry
        registryManager.getRegistryLock().lock();
        try {
            // double check
            // fix https://github.com/apache/dubbo/issues/7265.
            defaultNopRegistry = registryManager.getDefaultNopRegistryIfDestroyed();
            if (null != defaultNopRegistry) {
                return defaultNopRegistry;
            }
            registry = registryManager.getRegistry(key);
            if (registry != null) {
                return registry;
            }
            //create registry by spi/ioc
            registry = createRegistry(url);
        } catch (Exception e) {
            if (check) {
                throw new RuntimeException("Can not create registry " + url, e);
            } else {
                LOGGER.warn("Failed to obtain or create registry ", e);
            }
        } finally {
            // Release the lock
            registryManager.getRegistryLock().unlock();
        }

        if (check && registry == null) {
            throw new IllegalStateException("Can not create registry " + url);
        }

        if (registry != null) {
            registryManager.putRegistry(key, registry);
        }
        return registry;
    }

    /**
     * Create the key for the registries cache.
     * This method may be override by the sub-class.
     *
     * @param url the registration {@link URL url}
     * @return non-null
     */
    protected String createRegistryCacheKey(URL url) {
        return url.toServiceStringWithoutResolving();
    }

    protected abstract Registry createRegistry(URL url);


}
