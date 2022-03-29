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

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.extension.SPI;

public class ScopeModelUtil {

    public static <T> ScopeModel getOrDefault(ScopeModel scopeModel, Class<T> type) {
        if (scopeModel != null) {
            return scopeModel;
        }
        return getDefaultScopeModel(type);
    }

    private static <T> ScopeModel getDefaultScopeModel(Class<T> type) {
        SPI spi = type.getAnnotation(SPI.class);
        if (spi == null) {
            throw new IllegalArgumentException("SPI annotation not found for class: " + type.getName());
        }
        switch (spi.scope()) {
            case FRAMEWORK:
                return FrameworkModel.defaultModel();
            case APPLICATION:
                return ApplicationModel.defaultModel();
            case MODULE:
                return ApplicationModel.defaultModel().getDefaultModule();
            default:
                throw new IllegalStateException("Unable to get default scope model for type: " + type.getName());
        }
    }

    public static ModuleModel getModuleModel(ScopeModel scopeModel) {
        if (scopeModel == null) {
            return ApplicationModel.defaultModel().getDefaultModule();
        }
        if (scopeModel instanceof ModuleModel) {
            return (ModuleModel) scopeModel;
        } else {
            throw new IllegalArgumentException("Unable to get ModuleModel from " + scopeModel);
        }
    }

    public static ApplicationModel getApplicationModel(ScopeModel scopeModel) {
        return getOrDefaultApplicationModel(scopeModel);
    }

    public static ApplicationModel getOrDefaultApplicationModel(ScopeModel scopeModel) {
        if (scopeModel == null) {
            return ApplicationModel.defaultModel();
        }
        return getOrNullApplicationModel(scopeModel);
    }

    public static ApplicationModel getOrNullApplicationModel(ScopeModel scopeModel) {
        if (scopeModel == null) {
            return null;
        }
        if (scopeModel instanceof ApplicationModel) {
            return (ApplicationModel) scopeModel;
        } else if (scopeModel instanceof ModuleModel) {
            ModuleModel moduleModel = (ModuleModel) scopeModel;
            return moduleModel.getApplicationModel();
        } else {
            throw new IllegalArgumentException("Unable to get ApplicationModel from " + scopeModel);
        }
    }

    public static FrameworkModel getFrameworkModel(ScopeModel scopeModel) {
        if (scopeModel == null) {
            return FrameworkModel.defaultModel();
        }
        if (scopeModel instanceof ApplicationModel) {
            return ((ApplicationModel) scopeModel).getFrameworkModel();
        } else if (scopeModel instanceof ModuleModel) {
            ModuleModel moduleModel = (ModuleModel) scopeModel;
            return moduleModel.getApplicationModel().getFrameworkModel();
        } else if (scopeModel instanceof FrameworkModel) {
            return (FrameworkModel) scopeModel;
        } else {
            throw new IllegalArgumentException("Unable to get FrameworkModel from " + scopeModel);
        }
    }

    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type, ScopeModel scopeModel) {
        if (scopeModel != null) {
            return scopeModel.getExtensionLoader(type);
        } else {
            SPI spi = type.getAnnotation(SPI.class);
            if (spi == null) {
                throw new IllegalArgumentException("SPI annotation not found for class: " + type.getName());
            }
            switch (spi.scope()) {
                case FRAMEWORK:
                    return FrameworkModel.defaultModel().getExtensionLoader(type);
                case APPLICATION:
                    return ApplicationModel.defaultModel().getExtensionLoader(type);
                case MODULE:
                    return ApplicationModel.defaultModel().getDefaultModule().getExtensionLoader(type);
                default:
                    throw new IllegalArgumentException("Unable to get ExtensionLoader for type: " + type.getName());
            }
        }
    }
}
