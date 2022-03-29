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
package org.apache.dubbo.common.utils;

import org.apache.dubbo.common.extension.DubboInternalLoadingStrategy;
import org.apache.dubbo.common.extension.director.FooAppProvider;
import org.apache.dubbo.common.resource.GlobalResourcesRepository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * {@link ClassLoaderResourceLoader}
 */
public class ClassLoaderResourceLoaderTest {

    @Test
    public void test() {
        DubboInternalLoadingStrategy dubboInternalLoadingStrategy = new DubboInternalLoadingStrategy();
        String directory = dubboInternalLoadingStrategy.directory();
        String type = FooAppProvider.class.getName();
        String fileName = directory + type;

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Map<ClassLoader, Set<URL>> loadResources = ClassLoaderResourceLoader.loadResources(fileName, Arrays.asList(contextClassLoader));
        Assertions.assertTrue(loadResources.containsKey(contextClassLoader));
        Assertions.assertTrue(!loadResources.get(contextClassLoader).isEmpty());

        // cache
        Assertions.assertNotNull(ClassLoaderResourceLoader.getClassLoaderResourcesCache());
        loadResources = ClassLoaderResourceLoader.loadResources(fileName, Arrays.asList(contextClassLoader));
        Assertions.assertTrue(loadResources.containsKey(contextClassLoader));
        Assertions.assertTrue(!loadResources.get(contextClassLoader).isEmpty());

        Assertions.assertNotNull(GlobalResourcesRepository.getGlobalReusedDisposables());

        ClassLoaderResourceLoader.destroy();
        Assertions.assertNull(ClassLoaderResourceLoader.getClassLoaderResourcesCache());
    }
}
