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

import org.apache.dubbo.rpc.model.FrameworkModel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DefaultParameterNameReader implements ParameterNameReader {

    private final Map<Object, Optional<String[]>> cache = CollectionUtils.newConcurrentHashMap();
    private final List<ParameterNameReader> readers;

    public DefaultParameterNameReader(FrameworkModel frameworkModel) {
        readers = frameworkModel.getActivateExtensions(ParameterNameReader.class);
    }

    @Override
    public String[] readParameterNames(Method method) {
        return cache.computeIfAbsent(method, k -> {
                    String[] names = readByReflection(method.getParameters());
                    if (names == null) {
                        for (ParameterNameReader reader : readers) {
                            names = reader.readParameterNames(method);
                            if (names != null) {
                                break;
                            }
                        }
                    }
                    return Optional.ofNullable(names);
                })
                .orElse(null);
    }

    @Override
    public String[] readParameterNames(Constructor<?> ctor) {
        return cache.computeIfAbsent(ctor, k -> {
                    String[] names = readByReflection(ctor.getParameters());
                    if (names == null) {
                        for (ParameterNameReader reader : readers) {
                            names = reader.readParameterNames(ctor);
                            if (names != null) {
                                break;
                            }
                        }
                    }
                    return Optional.ofNullable(names);
                })
                .orElse(null);
    }

    private static String[] readByReflection(Parameter[] parameters) {
        int len = parameters.length;
        if (len == 0) {
            return StringUtils.EMPTY_STRING_ARRAY;
        }
        String[] names = new String[len];
        for (int i = 0; i < len; i++) {
            Parameter param = parameters[i];
            if (!param.isNamePresent()) {
                return null;
            }
            names[i] = param.getName();
        }
        return names;
    }
}
