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
package org.apache.dubbo.common.serialization;

import org.apache.dubbo.common.utils.ConcurrentHashSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClassHolder {
    private final Map<String, Set<Class<?>>> classCache = new ConcurrentHashMap<>();

    public void storeClass(Class<?> clazz) {
        classCache
                .computeIfAbsent(clazz.getName(), k -> new ConcurrentHashSet<>())
                .add(clazz);
    }

    public Class<?> loadClass(String className, ClassLoader classLoader) {
        Set<Class<?>> classList = classCache.get(className);
        if (classList == null) {
            return null;
        }
        for (Class<?> clazz : classList) {
            if (classLoader.equals(clazz.getClassLoader())) {
                return clazz;
            }
        }
        return null;
    }
}
