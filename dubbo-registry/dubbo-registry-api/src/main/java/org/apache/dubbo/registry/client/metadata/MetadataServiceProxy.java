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
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.proxy.InvokerInvocationHandler;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import static java.lang.reflect.Proxy.newProxyInstance;
import static org.apache.dubbo.registry.client.metadata.MetadataServiceURLBuilder.INSTANCE;

/**
 * The Proxy object for the {@link MetadataService} whose {@link ServiceInstance} providers may export multiple
 * {@link Protocol protocols} at the same time.
 *
 * @see ServiceInstance
 * @see MetadataService
 * @since 2.7.3
 */
class MetadataServiceProxy implements MetadataService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<URL> urls;

    private final Protocol protocol;

    public MetadataServiceProxy(ServiceInstance serviceInstance, Protocol protocol) {
        this(INSTANCE.build(serviceInstance), protocol);
    }

    public MetadataServiceProxy(List<URL> urls, Protocol protocol) {
        this.urls = urls;
        this.protocol = protocol;
    }

    @Override
    public String serviceName() {
        return doInMetadataService(MetadataService::serviceName);
    }

    @Override
    public List<String> getSubscribedURLs() {
        return doInMetadataService(MetadataService::getSubscribedURLs);
    }

    @Override
    public List<String> getExportedURLs(String serviceInterface, String group, String version, String protocol) {
        return doInMetadataService(metadataService ->
                metadataService.getExportedURLs(serviceInterface, group, version, protocol));
    }

    protected <T> T doInMetadataService(Function<MetadataService, T> callback) {

        T result = null;            // execution result

        Throwable exception = null; // exception maybe present

        Iterator<URL> iterator = urls.iterator();

        while (iterator.hasNext()) { // Executes MetadataService's method until success
            URL url = iterator.next();
            Invoker<MetadataService> invoker = null;
            try {
                invoker = this.protocol.refer(MetadataService.class, url);
                MetadataService proxy = (MetadataService) newProxyInstance(getClass().getClassLoader(),
                        new Class[]{MetadataService.class}, new InvokerInvocationHandler(invoker));
                result = callback.apply(proxy);
                exception = null;
            } catch (Throwable e) {
                exception = e;
                // If met with some error, invoke next
                if (logger.isErrorEnabled()) {
                    logger.error(e.getMessage(), e);
                }
            } finally {
                if (invoker != null) {
                    // to destroy the Invoker finally
                    invoker.destroy();
                    invoker = null;
                }
            }
        }

        if (exception != null) { // If all executions were failed
            throw new RuntimeException(exception.getMessage(), exception);
        }

        return result;
    }
}
