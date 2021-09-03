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
package org.apache.dubbo.common.beans.factory;

import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModel;
import org.apache.dubbo.rpc.model.ScopeModelPostProcessor;

/**
 * Initialize the bean factory for ScopeModel
 */
public abstract class ScopeBeanFactoryInitializer implements ScopeModelPostProcessor {

    @Override
    public void postProcessScopeModel(ScopeModel scopeModel) {
        if (scopeModel instanceof ApplicationModel) {
            ApplicationModel applicationModel = (ApplicationModel) scopeModel;
            registerApplicationBeans(applicationModel, applicationModel.getBeanFactory());
        } else if (scopeModel instanceof FrameworkModel) {
            FrameworkModel frameworkModel = (FrameworkModel) scopeModel;
            registerFrameworkBeans(frameworkModel, frameworkModel.getBeanFactory());
        } else if (scopeModel instanceof ModuleModel) {
            ModuleModel moduleModel = (ModuleModel) scopeModel;
            registerModuleBeans(moduleModel, moduleModel.getBeanFactory());
        }
    }

    /**
     * Initialize beans for framework
     *
     * @param frameworkModel
     * @param beanFactory
     */
    protected void registerFrameworkBeans(FrameworkModel frameworkModel, ScopeBeanFactory beanFactory) {

    }

    /**
     * Initialize beans for application
     *
     * @param applicationModel
     * @param beanFactory
     */
    protected void registerApplicationBeans(ApplicationModel applicationModel, ScopeBeanFactory beanFactory) {
//        beanFactory.registerBean(MetadataReportInstance.class);
//        beanFactory.registerBean(RemoteMetadataServiceImpl.class);
    }

    /**
     * Initialize beans for module
     *
     * @param moduleModel
     * @param beanFactory
     */
    protected void registerModuleBeans(ModuleModel moduleModel, ScopeBeanFactory beanFactory) {

    }

}
