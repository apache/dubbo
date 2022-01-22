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

import org.apache.dubbo.common.resource.GlobalResourcesRepository;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ClassLoaderResourceLoader {

    private static SoftReference<Map<ClassLoader, Map<String, Set<URL>>>> classLoaderResourcesCache = null;

    static {
        // register resources destroy listener
        GlobalResourcesRepository.registerGlobalDisposable(()-> destroy());
    }

    public static Map<ClassLoader, Set<URL>> loadResources(String fileName, List<ClassLoader> classLoaders) {
        Map<ClassLoader, Set<URL>> resources = new ConcurrentHashMap<>();
        CountDownLatch countDownLatch = new CountDownLatch(classLoaders.size());
        for (ClassLoader classLoader : classLoaders) {
            GlobalResourcesRepository.getGlobalExecutorService().submit(() -> {
                resources.put(classLoader, loadResources(fileName, classLoader));
                countDownLatch.countDown();
            });
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(resources));
    }

    public static Set<URL> loadResources(String fileName, ClassLoader currentClassLoader) {
        Map<ClassLoader, Map<String, Set<URL>>> classLoaderCache;
        if (classLoaderResourcesCache == null || (classLoaderCache = classLoaderResourcesCache.get()) == null) {
            synchronized (ClassLoaderResourceLoader.class) {
                if (classLoaderResourcesCache == null || (classLoaderCache = classLoaderResourcesCache.get()) == null) {
                    classLoaderCache = new ConcurrentHashMap<>();
                    classLoaderResourcesCache = new SoftReference<>(classLoaderCache);
                }
            }
        }
        if (!classLoaderCache.containsKey(currentClassLoader)) {
            classLoaderCache.putIfAbsent(currentClassLoader, new ConcurrentHashMap<>());
        }
        Map<String, Set<URL>> urlCache = classLoaderCache.get(currentClassLoader);
        if (!urlCache.containsKey(fileName)) {
            Set<URL> set = new LinkedHashSet<>();
            Enumeration<URL> urls;
            try {
                urls = currentClassLoader.getResources(fileName);
                boolean isNative = NativeUtils.isNative();
                if (urls != null) {
                    while (urls.hasMoreElements()) {
                        URL url = urls.nextElement();
                        if (isNative) {
                            //In native mode, the address of each URL is the same instead of different paths, so it is necessary to set the ref to make it different
                            setRef(url);
                        }
                        set.add(url);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            urlCache.put(fileName, set);
        }
        return urlCache.get(fileName);
    }

    public static void destroy() {
        synchronized (ClassLoaderResourceLoader.class) {
            classLoaderResourcesCache = null;
        }
    }

    private static void setRef(URL url) {
        try {
            Field field = URL.class.getDeclaredField("ref");
            field.setAccessible(true);
            field.set(url, UUID.randomUUID().toString());
        } catch (Throwable ignore) {
        }
    }


    // for test
    protected static SoftReference<Map<ClassLoader, Map<String, Set<URL>>>> getClassLoaderResourcesCache() {
        return classLoaderResourcesCache;
    }
}
