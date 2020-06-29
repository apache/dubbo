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
package org.apache.dubbo.registry.client.metadata.proxy;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The composite implementation of {@link MetadataServiceProxyFactory}
 *
 * @since 2.7.8
 */
public class CompositeMetadataServiceProxyFactory extends BaseMetadataServiceProxyFactory {

    private static final Logger logger = LoggerFactory.getLogger(CompositeMetadataServiceProxyFactory.class);

    @Override
    public MetadataService createProxy(ServiceInstance serviceInstance) {
        MetadataService metadataService = (MetadataService) newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{MetadataService.class},
                new MetadataServiceInvocationHandler(serviceInstance, this)
        );
        return metadataService;
    }

    static class MetadataServiceInvocationHandler implements InvocationHandler {

        private final ServiceInstance serviceInstance;

        private final MetadataServiceProxyFactory excluded;

        private volatile List<MetadataService> metadataServices;

        MetadataServiceInvocationHandler(ServiceInstance serviceInstance,
                                         MetadataServiceProxyFactory excluded) {
            this.serviceInstance = serviceInstance;
            this.excluded = excluded;
        }

        private List<MetadataService> loadMetadataServices() {
            return getExtensionLoader(MetadataServiceProxyFactory.class)
                    .getSupportedExtensionInstances()
                    .stream()
                    .filter(this::isRequiredFactory)
                    .map(this::getProxy)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        private List<MetadataService> getMetadataServices() {
            if (metadataServices == null) {
                metadataServices = loadMetadataServices();
                if (metadataServices.isEmpty()) {
                    throw new IllegalStateException(format("No valid proxy of %s can't be loaded.",
                            MetadataService.class.getName()));
                }
            }
            return metadataServices;
        }

        private boolean isRequiredFactory(MetadataServiceProxyFactory factory) {
            return !factory.equals(excluded);
        }

        private MetadataService getProxy(MetadataServiceProxyFactory factory) {
            MetadataService metadataService = null;
            try {
                metadataService = factory.getProxy(serviceInstance);
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error(format("The proxy of %s can't be gotten by %s [from : %s].",
                            MetadataService.class.getName(),
                            factory.getClass().getName(),
                            serviceInstance.toString()));
                }
            }
            return metadataService;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(proxy, args);
            }

            Object result = null;

            for (MetadataService metadataService : getMetadataServices()) {
                try {
                    result = method.invoke(metadataService, args);
                    if (result != null) {
                        break;
                    }
                } catch (Exception e) {
                    if (logger.isErrorEnabled()) {
                        logger.error(format("MetadataService[type : %s] executes failed.", metadataService.getClass().getName()), e);
                    }
                }
            }

            return result;
        }
    }

}
