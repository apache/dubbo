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

package org.apache.dubbo.rpc.proxy.bytebuddy;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.MAX_PROXY_COUNT;

public class ByteBuddyProxy {

    private static final Map<ClassLoader, Map<CacheKey, ByteBuddyProxy>> PROXY_CACHE_MAP = new WeakHashMap<>();

    private final Class<?> proxyClass;

    private final InvocationHandler handler;

    private ByteBuddyProxy(Class<?> proxyClass, InvocationHandler handler) {
        this.proxyClass = proxyClass;
        this.handler = handler;
    }

    public static Object newInstance(ClassLoader cl, Class<?>[] interfaces, InvocationHandler handler) {
        return getProxy(cl, interfaces, handler).newInstance();
    }

    private static ByteBuddyProxy getProxy(ClassLoader cl, Class<?>[] interfaces, InvocationHandler handler) {
        if (interfaces.length > MAX_PROXY_COUNT) {
            throw new IllegalArgumentException("interface limit exceeded");
        }
        interfaces = interfaces.clone();
        Arrays.sort(interfaces, Comparator.comparing(Class::getName));
        CacheKey key = new CacheKey(interfaces);
        // get cache by class loader.
        final Map<CacheKey, ByteBuddyProxy> cache;
        synchronized (PROXY_CACHE_MAP) {
            cache = PROXY_CACHE_MAP.computeIfAbsent(cl, k -> new ConcurrentHashMap<>());
        }

        ByteBuddyProxy proxy = cache.get(key);
        if (proxy == null) {
            synchronized (interfaces[0]) {
                proxy = cache.get(key);
                if (proxy == null) {
                    // create ByteBuddyProxy class.
                    proxy = new ByteBuddyProxy(buildProxyClass(cl, interfaces, handler), handler);
                    cache.put(key, proxy);
                }
            }
        }
        return proxy;
    }

    private Object newInstance() {
        try {
            Constructor<?> constructor = proxyClass.getDeclaredConstructor(InvocationHandler.class);
            return constructor.newInstance(handler);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> buildProxyClass(ClassLoader cl, Class<?>[] ics, InvocationHandler handler) {
        ElementMatcher.Junction<ByteCodeElement> methodMatcher = Arrays.stream(ics)
            .map(ElementMatchers::isDeclaredBy).reduce(ElementMatcher.Junction::or)
            .orElse(ElementMatchers.none()).and(ElementMatchers
                .not(ElementMatchers.isDeclaredBy(Object.class)));
        return new ByteBuddy()
            .subclass(Proxy.class)
            .implement(ics)
            .method(methodMatcher)
            .intercept(MethodDelegation.to(new ByteBuddyInterceptor(handler)))
            .make()
            .load(cl)
            .getLoaded();
    }

    private static class CacheKey {

        private final Class<?>[] classes;

        private CacheKey(Class<?>[] classes) {
            this.classes = classes;
        }

        public Class<?>[] getClasses() {
            return classes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey that = (CacheKey) o;
            return Arrays.equals(classes, that.classes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(classes);
        }
    }
}
