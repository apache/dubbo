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

import org.apache.dubbo.common.extension.ExtensionPostProcessor;

import java.util.concurrent.atomic.AtomicBoolean;

public class ScopeModelAwareExtensionProcessor implements ExtensionPostProcessor {
    private ScopeModel scopeModel;
    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private ModuleModel moduleModel;
    private final AtomicBoolean inited = new AtomicBoolean(false);

    public ScopeModelAwareExtensionProcessor(ScopeModel scopeModel) {
        this.scopeModel = scopeModel;
    }

    private void init() {
        if (!inited.compareAndSet(false, true)) {
            return;
        }
        frameworkModel = ScopeModelUtil.getFrameworkModel(scopeModel);
        applicationModel = ScopeModelUtil.getApplicationModel(scopeModel);
        moduleModel = ScopeModelUtil.getModuleModel(scopeModel);
    }

    @Override
    public Object postProcessAfterInitialization(Object instance, String name) throws Exception {
        init();
        if (instance instanceof ScopeModelAware) {
            ScopeModelAware modelAware = (ScopeModelAware) instance;
            modelAware.setScopeModel(scopeModel);
            if (this.moduleModel != null) {
                modelAware.setModuleModel(this.moduleModel);
            }
            if (this.applicationModel != null) {
                modelAware.setApplicationModel(this.applicationModel);
            }
            if (this.frameworkModel != null) {
                modelAware.setFrameworkModel(this.frameworkModel);
            }
        }
        return instance;
    }

}
