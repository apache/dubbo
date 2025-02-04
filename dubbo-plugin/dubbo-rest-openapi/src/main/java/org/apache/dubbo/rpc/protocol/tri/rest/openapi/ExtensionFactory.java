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
package org.apache.dubbo.rpc.protocol.tri.rest.openapi;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Pair;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public final class ExtensionFactory {

    private final ExtensionLoader<OpenAPIExtension> extensionLoader;
    private final List<OpenAPIExtension> extensions;
    private final Map<Object, Object> cache;

    public ExtensionFactory(FrameworkModel frameworkModel) {
        extensionLoader = frameworkModel.getExtensionLoader(OpenAPIExtension.class);
        extensions = extensionLoader.getActivateExtensions();
        cache = CollectionUtils.newConcurrentHashMap();
    }

    public <T extends OpenAPIExtension> boolean hasExtensions(Class<T> type) {
        return getExtensions(type).length > 0;
    }

    public <T extends OpenAPIExtension> T[] getExtensions(Class<T> type) {
        return (T[]) cache.computeIfAbsent(type, k -> {
            List<OpenAPIExtension> list = new ArrayList<>();
            for (OpenAPIExtension extension : extensions) {
                if (extension instanceof Supplier) {
                    extension = ((Supplier<T>) extension).get();
                }
                if (type.isInstance(extension)) {
                    list.add(extension);
                }
            }
            return list.toArray((T[]) Array.newInstance(type, list.size()));
        });
    }

    public <T extends OpenAPIExtension> T[] getExtensions(Class<T> type, String group) {
        if (group == null) {
            return getExtensions(type);
        }
        return (T[]) cache.computeIfAbsent(Pair.of(type, group), k -> {
            List<OpenAPIExtension> list = new ArrayList<>();
            for (OpenAPIExtension extension : extensions) {
                if (extension instanceof Supplier) {
                    extension = ((Supplier<T>) extension).get();
                }
                if (type.isInstance(extension) && accept(extension, group)) {
                    list.add(extension);
                }
            }
            return list.toArray((T[]) Array.newInstance(type, list.size()));
        });
    }

    public <T extends OpenAPIExtension> T getExtension(Class<T> type, String name) {
        OpenAPIExtension extension = extensionLoader.getExtension(name, true);
        if (extension instanceof Supplier) {
            extension = ((Supplier<T>) extension).get();
        }
        return type.isInstance(extension) ? (T) extension : null;
    }

    private static boolean accept(OpenAPIExtension extension, String group) {
        String[] groups = extension.getGroups();
        return groups == null || groups.length == 0 || Arrays.asList(groups).contains(group);
    }
}
