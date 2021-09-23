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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.common.config.ReferenceCache;
import org.apache.dubbo.config.ReferenceConfigBase;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.List;

/**
 * A impl of ReferenceCache for Application
 */
public class CompositeReferenceCache implements ReferenceCache {
    private ApplicationModel applicationModel;

    public CompositeReferenceCache(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public <T> T get(ReferenceConfigBase<T> referenceConfig) {
        return referenceConfig.get();
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            T proxy = moduleModel.getDeployer().getReferenceCache().get(key, type);
            if (proxy != null) {
                return proxy;
            }
        }
        return null;
    }

    @Override
    public <T> T get(String key) {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            T proxy = moduleModel.getDeployer().getReferenceCache().get(key);
            if (proxy != null) {
                return proxy;
            }
        }
        return null;
    }

    @Override
    public <T> List<T> getAll(Class<T> type) {
        List<T> proxies = new ArrayList<>();
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            proxies.addAll(moduleModel.getDeployer().getReferenceCache().getAll(type));
        }
        return proxies;
    }

    @Override
    public <T> T get(Class<T> type) {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            T proxy = moduleModel.getDeployer().getReferenceCache().get(type);
            if (proxy != null) {
                return proxy;
            }
        }
        return null;
    }

    @Override
    public void destroy(String key, Class<?> type) {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            moduleModel.getDeployer().getReferenceCache().destroy(key, type);
        }
    }

    @Override
    public void destroy(Class<?> type) {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            moduleModel.getDeployer().getReferenceCache().destroy(type);
        }
    }

    @Override
    public <T> void destroy(ReferenceConfigBase<T> referenceConfig) {
        referenceConfig.getScopeModel().getDeployer().getReferenceCache().destroy(referenceConfig);
    }

    @Override
    public void destroyAll() {
        for (ModuleModel moduleModel : applicationModel.getModuleModels()) {
            moduleModel.getDeployer().getReferenceCache().destroyAll();
        }
    }
}
