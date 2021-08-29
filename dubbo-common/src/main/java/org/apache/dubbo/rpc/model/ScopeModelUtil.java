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

public class ScopeModelUtil {

    public static ModuleModel getModuleModel(ScopeModel scopeModel) {
        if (scopeModel instanceof ModuleModel) {
            return (ModuleModel) scopeModel;
        }
        if (scopeModel == null) {
            return ApplicationModel.defaultModel().getDefaultModule();
        }
        return null;
    }

    public static ApplicationModel getApplicationModel(ScopeModel scopeModel) {
        if (scopeModel instanceof ApplicationModel) {
            return (ApplicationModel) scopeModel;
        } else if (scopeModel instanceof ModuleModel) {
            ModuleModel moduleModel = (ModuleModel) scopeModel;
            return moduleModel.getApplicationModel();
        }
        if (scopeModel == null) {
            return ApplicationModel.defaultModel();
        }
        return null;
    }

    public static FrameworkModel getFrameworkModel(ScopeModel scopeModel) {
        if (scopeModel instanceof ApplicationModel) {
            return ((ApplicationModel) scopeModel).getFrameworkModel();
        } else if (scopeModel instanceof ModuleModel) {
            ModuleModel moduleModel = (ModuleModel) scopeModel;
            return moduleModel.getApplicationModel().getFrameworkModel();
        } else if (scopeModel instanceof FrameworkModel) {
            return (FrameworkModel) scopeModel;
        }
        if (scopeModel == null) {
            return FrameworkModel.defaultModel();
        }
        return null;
    }

}
