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
package org.apache.dubbo.rpc.protocol.tri.rest.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class MethodWalker {

    private final Set<Class<?>> classes = new LinkedHashSet<>();
    private final Map<Key, List<Method>> methodsMap = new HashMap<>();

    public void walk(Class<?> clazz, BiConsumer<Set<Class<?>>, Consumer<Consumer<List<Method>>>> visitor) {
        if (clazz.getName().contains("$$")) {
            clazz = clazz.getSuperclass();
        }

        walkHierarchy(clazz);

        visitor.accept(classes, consumer -> {
            for (Map.Entry<Key, List<Method>> entry : methodsMap.entrySet()) {
                consumer.accept(entry.getValue());
            }
        });
    }

    private void walkHierarchy(Class<?> clazz) {
        if (classes.isEmpty() || clazz.getDeclaredAnnotations().length > 0) {
            classes.add(clazz);
        }
        for (Method method : clazz.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if ((modifiers & (Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC) {
                methodsMap
                        .computeIfAbsent(Key.of(method), k -> new ArrayList<>())
                        .add(method);
            }
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            walkHierarchy(superClass);
        }
        for (Class<?> itf : clazz.getInterfaces()) {
            walkHierarchy(itf);
        }
    }

    private static final class Key {
        private final String name;
        private final Class<?>[] parameterTypes;

        private Key(String name, Class<?>[] parameterTypes) {
            this.name = name;
            this.parameterTypes = parameterTypes;
        }

        private static Key of(Method method) {
            return new Key(method.getName(), method.getParameterTypes());
        }

        @Override
        @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass", "EqualsDoesntCheckParameterClass"})
        public boolean equals(Object obj) {
            Key key = (Key) obj;
            return name.equals(key.name) && Arrays.equals(parameterTypes, key.parameterTypes);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            for (Class<?> type : parameterTypes) {
                result = 31 * result + type.hashCode();
            }
            return result;
        }

        @Override
        public String toString() {
            return name + Arrays.toString(parameterTypes);
        }
    }
}
