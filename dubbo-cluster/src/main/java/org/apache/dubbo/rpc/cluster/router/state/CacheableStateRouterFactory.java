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
package org.apache.dubbo.rpc.cluster.router.state;

import org.apache.dubbo.common.URL;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * If you want to provide a router implementation based on design of v2.7.0, please extend from this abstract class.
 * For 2.6.x style router, please implement and use RouterFactory directly.
 */
public abstract class CacheableStateRouterFactory implements StateRouterFactory {
    // TODO reuse StateRouter for all routerChain
    private final ConcurrentMap<String, StateRouter> routerMap = new ConcurrentHashMap<>();

    @Override
    public <T> StateRouter<T> getRouter(Class<T> interfaceClass, URL url) {
        return routerMap.computeIfAbsent(url.getServiceKey(), k -> createRouter(interfaceClass, url));
    }

    protected abstract <T> StateRouter<T> createRouter(Class<T> interfaceClass, URL url);
}
