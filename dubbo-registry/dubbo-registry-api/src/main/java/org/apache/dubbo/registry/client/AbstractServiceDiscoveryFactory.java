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
package org.apache.dubbo.registry.client;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractServiceDiscoveryFactory implements ServiceDiscoveryFactory {

    private static final Logger logger = LoggerFactory.getLogger(AbstractServiceDiscoveryFactory.class);

    private static ConcurrentMap<String, ServiceDiscovery> discoveries = new ConcurrentHashMap<>();

    public static Collection<ServiceDiscovery> getDiscoveries() {
        return Collections.unmodifiableCollection(discoveries.values());
    }

    /**
     * Close all created registries
     */
    public static void destroyAll() {
        if (logger.isInfoEnabled()) {
            logger.info("Closing all ServiceDicovery instances: " + getDiscoveries());
        }

        for (ServiceDiscovery discovery : getDiscoveries()) {
            try {
                discovery.stop();
            } catch (Throwable e) {
                logger.error("Error trying to close ServiceDiscovery instance.", e);
            }
        }
        discoveries.clear();
    }

    /**
     * @param url "zookeeper://ip:port/RegistryService?xxx"
     * @return
     */
    @Override
    public ServiceDiscovery getDiscovery(URL url) {
        String key = url.toServiceStringWithoutResolving();

        return discoveries.computeIfAbsent(key, k -> {
            ServiceDiscovery discovery = createDiscovery(url);
            return new EventPublishingServiceDiscovery(discovery);
        });
    }

    protected abstract ServiceDiscovery createDiscovery(URL url);
}
