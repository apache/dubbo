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
package com.alibaba.dubbo.config.spring.registry;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.RegistryFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MockRegistryFactory implements RegistryFactory {

    private static final Map<URL, Registry> registries = new HashMap<URL, Registry>();

    public static Collection<Registry> getCachedRegistry() {
        return registries.values();
    }

    public static void cleanCachedRegistry() {
        registries.clear();
    }

    public Registry getRegistry(URL url) {
        MockRegistry registry = new MockRegistry(url);
        registries.put(url, registry);
        return registry;
    }
}
