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
package org.apache.dubbo.rpc.protocol.tri;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.PathResolver;

import java.util.Map;

public class TriplePathResolver implements PathResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriplePathResolver.class);

    private final Map<String, Invoker<?>> mapping = CollectionUtils.newConcurrentHashMap();
    private final Map<String, Boolean> nativeStubs = CollectionUtils.newConcurrentHashMap();

    @Override
    public void register(Invoker<?> invoker) {
        URL url = invoker.getUrl();
        String serviceKey = url.getServiceKey();
        String serviceInterface = url.getServiceModel().getServiceModel().getInterfaceName();

        register0(serviceKey, serviceInterface, invoker, url);

        // Path patten: '{interfaceName}' or '{contextPath}/{interfaceName}'
        String path = url.getPath();
        int index = path.lastIndexOf('/');
        if (index == -1) {
            return;
        }
        String fallbackPath = path.substring(0, index + 1) + serviceInterface;
        register0(URL.buildKey(path, url.getGroup(), url.getVersion()), fallbackPath, invoker, url);
    }

    private void register0(String path, String fallbackPath, Invoker<?> invoker, URL url) {
        // register default mapping
        Invoker<?> previous = mapping.put(path, invoker);
        if (previous != null) {
            if (path.equals(fallbackPath)) {
                LOGGER.info(
                        "Already exists an invoker[{}] on path[{}], dubbo will override with invoker[{}]",
                        previous.getUrl(),
                        path,
                        url);
            } else {
                throw new IllegalStateException(String.format(
                        "Already exists an invoker[%s] on path[%s], failed to add invoker[%s], please use a unique path.",
                        previous.getUrl(), path, url));
            }
        } else {
            LOGGER.debug(
                    "Register triple grpc mapping: '{}' -> invoker[{}], service=[{}]",
                    path,
                    url,
                    ClassUtils.toShortString(url.getServiceModel().getProxyObject()));
        }

        // register fallback mapping
        if (TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT && !path.equals(fallbackPath)) {
            previous = mapping.putIfAbsent(fallbackPath, invoker);
            if (previous != null) {
                LOGGER.info(
                        "Already exists an invoker[{}] on path[{}], dubbo will skip override with invoker[{}]",
                        previous.getUrl(),
                        fallbackPath,
                        url);
            } else {
                LOGGER.info(
                        "Register fallback triple grpc mapping: '{}' -> invoker[{}], service=[{}]",
                        fallbackPath,
                        url,
                        ClassUtils.toShortString(url.getServiceModel().getProxyObject()));
            }
        }
    }

    @Override
    public void unregister(Invoker<?> invoker) {
        URL url = invoker.getUrl();
        mapping.remove(url.getServiceKey());
        if (TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT) {
            mapping.remove(url.getServiceModel().getServiceModel().getInterfaceName());
        }
    }

    @Override
    public Invoker<?> add(String path, Invoker<?> invoker) {
        return mapping.put(path, invoker);
    }

    @Override
    public Invoker<?> addIfAbsent(String path, Invoker<?> invoker) {
        return mapping.putIfAbsent(path, invoker);
    }

    @Override
    public Invoker<?> resolve(String path, String group, String version) {
        Invoker<?> invoker = mapping.get(URL.buildKey(path, group, version));
        if (invoker == null && TripleProtocol.RESOLVE_FALLBACK_TO_DEFAULT) {
            invoker = mapping.get(URL.buildKey(path, group, TripleConstants.DEFAULT_VERSION));
            if (invoker == null) {
                invoker = mapping.get(path);
            }
        }
        return invoker;
    }

    public boolean hasNativeStub(String path) {
        return nativeStubs.containsKey(path);
    }

    @Override
    public void addNativeStub(String path) {
        nativeStubs.put(path, Boolean.TRUE);
    }

    @Override
    public void remove(String path) {
        mapping.remove(path);
    }

    @Override
    public void destroy() {
        mapping.clear();
    }
}
