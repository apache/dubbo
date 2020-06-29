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
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.reflect.Proxy.newProxyInstance;
import static org.apache.dubbo.common.extension.ExtensionLoader.getExtensionLoader;

/**
 * The composite implementation of {@link MetadataServiceProxyFactory}
 *
 * @since 2.7.8
 */
public class CompositeMetadataServiceProxyFactory implements MetadataServiceProxyFactory {

    @Override
    public MetadataService getProxy(ServiceInstance serviceInstance) {
        MetadataService metadataService = (MetadataService) newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{MetadataService.class},
                new MetadataServiceInvocationHandler(serviceInstance, this)
        );
        return metadataService;
    }

    static class MetadataServiceInvocationHandler implements InvocationHandler {

        private final List<MetadataService> metadataServices;

        private final Logger logger = LoggerFactory.getLogger(getClass());

        MetadataServiceInvocationHandler(ServiceInstance serviceInstance,
                                         MetadataServiceProxyFactory excluded) {
            this.metadataServices = initMetadataServices(serviceInstance, excluded);
        }

        private List<MetadataService> initMetadataServices(ServiceInstance serviceInstance,
                                                           MetadataServiceProxyFactory excluded) {
            return getExtensionLoader(MetadataServiceProxyFactory.class)
                    .getSupportedExtensionInstances()
                    .stream()
                    .filter(factory -> !factory.equals(excluded))
                    .map(factory -> factory.getProxy(serviceInstance))
                    .collect(Collectors.toList());
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            if (Object.class.equals(method.getDeclaringClass())) {
                return method.invoke(proxy, args);
            }

            Object result = null;

            for (MetadataService metadataService : metadataServices) {
                try {
                    result = method.invoke(metadataService, args);
                    if (result != null) {
                        break;
                    }
                } catch (Exception e) {
                    if (logger.isErrorEnabled()) {
                        logger.error(format("MetadataService[type : %s] executes failed", metadataService.getClass().getName()), e);
                    }
                }
            }

            return result;
        }
    }

}
