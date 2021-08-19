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

public class ModelAwarePostProcessor implements ExtensionPostProcessor {
    private Object model;
    private FrameworkModel frameworkModel;
    private ApplicationModel applicationModel;
    private ModuleModel moduleModel;

    public ModelAwarePostProcessor(Object model) {
        this.model = model;
        if (model instanceof FrameworkModel) {
            frameworkModel = (FrameworkModel) model;
        } else if (model instanceof ApplicationModel) {
            applicationModel = (ApplicationModel) model;
            frameworkModel = applicationModel.getFrameworkModel();
        } else if (model instanceof ModuleModel) {
            moduleModel = (ModuleModel) model;
            applicationModel = moduleModel.getApplicationModel();
            frameworkModel = applicationModel.getFrameworkModel();
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object instance, String name) throws Exception {
        if (instance instanceof ModelAware) {
            ModelAware modelAware = (ModelAware) instance;
            if (this.applicationModel != null) {
                modelAware.setApplicationModel(this.applicationModel);
            }
            if (this.moduleModel != null) {
                modelAware.setModuleModel(this.moduleModel);
            }
            if (this.frameworkModel != null) {
                modelAware.setFrameworkModel(this.frameworkModel);
            }
        }
        return instance;
    }
}
