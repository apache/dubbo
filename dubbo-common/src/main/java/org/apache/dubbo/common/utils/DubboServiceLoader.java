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

import org.apache.dubbo.common.lang.Prioritized;

import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * An extension class of {@link ServiceLoader} to help the ease for use of Dubbo services/components
 *
 * @see ServiceLoader
 * @see Prioritized
 * @since 2.7.4
 */
public class DubboServiceLoader {

    /**
     * Load a {@link Stream stream} of services by the specified {@link Class type} and {@link ClassLoader}
     *
     * @param serviceClass {@link Class Service class}
     * @param classLoader  {@link ClassLoader}
     * @param <S>          {@link Class Service type}
     * @return the {@link Stream} of services that has been sorted by {@link Prioritized#COMPARATOR}
     * @throws NullPointerException if <code>serviceClass</code> is <code>null</code>
     */
    public static <S> Stream<S> load(Class<S> serviceClass, ClassLoader classLoader) throws NullPointerException {
        if (serviceClass == null) {
            throw new NullPointerException("The serviceClass must not be null");
        }

        ClassLoader actualClassLoader = classLoader;
        if (actualClassLoader == null) {
            actualClassLoader = DubboServiceLoader.class.getClassLoader();
        }

        ServiceLoader<S> serviceLoader = ServiceLoader.load(serviceClass, actualClassLoader);

        return stream(serviceLoader.spliterator(), false).sorted(Prioritized.COMPARATOR);
    }

    /**
     * Load a {@link Stream stream} of services by the specified {@link Class type} under
     * {@link Thread#getContextClassLoader() the current thread context ClassLoader}
     *
     * @param serviceClass {@link Class Service class}
     * @param <S>          {@link Class Service type}
     * @return the {@link Stream} of services that has been sorted by {@link Prioritized#COMPARATOR}
     * @throws NullPointerException if <code>serviceClass</code> is <code>null</code>
     */
    public static <S> Stream<S> load(Class<S> serviceClass) throws NullPointerException {
        return load(serviceClass, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Load a {@link List list} of services by the specified {@link Class type} and {@link ClassLoader}
     *
     * @param serviceClass {@link Class Service class}
     * @param classLoader  {@link ClassLoader}
     * @param <S>          {@link Class Service type}
     * @return the {@link List list} of services that has been sorted by {@link Prioritized#COMPARATOR}
     * @throws NullPointerException
     */
    public static <S> List<S> loadServices(Class<S> serviceClass, ClassLoader classLoader) throws NullPointerException {
        return Collections.unmodifiableList(load(serviceClass, classLoader).collect(toList()));
    }

    /**
     * Load a {@link List list} of services by the specified {@link Class type} under
     * {@link Thread#getContextClassLoader() the current thread context ClassLoader}
     *
     * @param serviceClass {@link Class Service class}
     * @param <S>          {@link Class Service type}
     * @return the {@link List list} of services that has been sorted by {@link Prioritized#COMPARATOR}
     * @throws NullPointerException
     */
    public static <S> List<S> loadServices(Class<S> serviceClass) throws NullPointerException {
        return loadServices(serviceClass, Thread.currentThread().getContextClassLoader());
    }
}
