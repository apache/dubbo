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
package org.apache.dubbo.rpc.model;

import org.apache.dubbo.common.extension.ExtensionDirector;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.ExtensionScope;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * Model of dubbo framework, it can be shared with multiple applications.
 */
public class FrameworkModel {

    protected static final Logger LOGGER = LoggerFactory.getLogger(FrameworkModel.class);

    private volatile static FrameworkModel defaultInstance;

    private final ExtensionDirector extensionDirector;

    private Collection<ApplicationModel> applicationModels = Collections.synchronizedSet(new LinkedHashSet<>());

    public FrameworkModel() {
        extensionDirector = new ExtensionDirector(null, ExtensionScope.FRAMEWORK);
        extensionDirector.addExtensionPostProcessor(new ModelAwarePostProcessor(this));
    }

    public static FrameworkModel defaultModel() {
        if (defaultInstance == null) {
            synchronized (FrameworkModel.class) {
                if (defaultInstance == null) {
                    defaultInstance = new FrameworkModel();
                }
            }
        }
        return defaultInstance;
    }

    public ExtensionDirector getExtensionDirector() {
        return extensionDirector;
    }

    public <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        return extensionDirector.getExtensionLoader(type);
    }

    public void addApplication(ApplicationModel model) {
        this.applicationModels.add(model);
    }

    public void removeApplication(ApplicationModel model) {
        this.applicationModels.remove(model);
    }

    public Collection<ApplicationModel> getApplicationModels() {
        return applicationModels;
    }
}
