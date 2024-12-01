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
package org.apache.dubbo.common.extension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Uniform accessor for extension
 */
public interface ExtensionAccessor {

    ExtensionDirector getExtensionDirector();

    default <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return getExtensionDirector().getExtensionLoader(type);
    }

    default <T> T getExtension(Class<T> type, String name) {
        ExtensionLoader<T> extensionLoader = getExtensionLoader(type);
        return extensionLoader != null ? extensionLoader.getExtension(name) : null;
    }

    default <T> T getAdaptiveExtension(Class<T> type) {
        ExtensionLoader<T> extensionLoader = getExtensionLoader(type);
        return extensionLoader != null ? extensionLoader.getAdaptiveExtension() : null;
    }

    default <T> T getDefaultExtension(Class<T> type) {
        ExtensionLoader<T> extensionLoader = getExtensionLoader(type);
        return extensionLoader != null ? extensionLoader.getDefaultExtension() : null;
    }

    default <T> List<T> getActivateExtensions(Class<T> type) {
        ExtensionLoader<T> extensionLoader = getExtensionLoader(type);
        return extensionLoader != null ? extensionLoader.getActivateExtensions() : Collections.emptyList();
    }

    default <T> T getFirstActivateExtension(Class<T> type) {
        ExtensionLoader<T> extensionLoader = getExtensionLoader(type);
        if (extensionLoader == null) {
            throw new IllegalArgumentException("ExtensionLoader for [" + type + "] is not found");
        }
        List<T> extensions = extensionLoader.getActivateExtensions();
        if (extensions.isEmpty()) {
            throw new IllegalArgumentException("No activate extensions for [" + type + "] found");
        }
        return extensions.get(0);
    }

    default Set<String> getSupportedExtensions(Class<?> type) {
        ExtensionLoader<?> extensionLoader = getExtensionLoader(type);
        return extensionLoader != null ? extensionLoader.getSupportedExtensions() : Collections.emptySet();
    }
}
