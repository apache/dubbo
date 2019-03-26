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

package com.alibaba.dubbo.registry;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;

import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public interface Registry extends org.apache.dubbo.registry.Registry {

    @Override
    com.alibaba.dubbo.common.URL getUrl();

    void register(com.alibaba.dubbo.common.URL url);

    void unregister(com.alibaba.dubbo.common.URL url);

    void subscribe(com.alibaba.dubbo.common.URL url, com.alibaba.dubbo.registry.NotifyListener listener);

    void unsubscribe(com.alibaba.dubbo.common.URL url, com.alibaba.dubbo.registry.NotifyListener listener);

    List<com.alibaba.dubbo.common.URL> lookup(com.alibaba.dubbo.common.URL url);

    @Override
    default void register(URL url) {
        this.register(new com.alibaba.dubbo.common.URL(url));
    }

    @Override
    default void unregister(URL url) {
        this.unregister(new com.alibaba.dubbo.common.URL(url));
    }

    @Override
    default void subscribe(URL url, NotifyListener listener) {
        this.subscribe(new com.alibaba.dubbo.common.URL(url),
                new com.alibaba.dubbo.registry.NotifyListener.CompatibleNotifyListener(listener));
    }

    @Override
    default void unsubscribe(URL url, NotifyListener listener) {
        this.unsubscribe(new com.alibaba.dubbo.common.URL(url),
                new com.alibaba.dubbo.registry.NotifyListener.CompatibleNotifyListener(listener));
    }

    @Override
    default List<URL> lookup(URL url) {
        return this.lookup(new com.alibaba.dubbo.common.URL(url)).stream().map(u -> u.getOriginalURL()).
                collect(Collectors.toList());
    }
}
