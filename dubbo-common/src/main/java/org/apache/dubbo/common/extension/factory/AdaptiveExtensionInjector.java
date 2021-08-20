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
package org.apache.dubbo.common.extension.factory;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.ExtensionInjector;
import org.apache.dubbo.common.extension.ExtensionLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AdaptiveExtensionInjector
 */
@Adaptive
public class AdaptiveExtensionInjector implements ExtensionInjector {

    private final List<ExtensionInjector> factories;

    public AdaptiveExtensionInjector() {
        ExtensionLoader<ExtensionInjector> loader = ExtensionLoader.getExtensionLoader(ExtensionInjector.class);
        List<ExtensionInjector> list = new ArrayList<ExtensionInjector>();
        for (String name : loader.getSupportedExtensions()) {
            list.add(loader.getExtension(name));
        }
        factories = Collections.unmodifiableList(list);
    }

    @Override
    public <T> T getInstance(Class<T> type, String name) {
        for (ExtensionInjector factory : factories) {
            T extension = factory.getInstance(type, name);
            if (extension != null) {
                return extension;
            }
        }
        return null;
    }

}
