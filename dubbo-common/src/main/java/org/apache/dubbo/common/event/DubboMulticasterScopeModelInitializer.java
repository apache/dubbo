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
package org.apache.dubbo.common.event;

import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Initialize {@link DubboLifecycleEventMulticaster} for {@link ApplicationModel}
 *
 * @see DubboLifecycleEventMulticaster
 * @since 3.3.0
 */
public class DubboMulticasterScopeModelInitializer implements ScopeModelInitializer {

    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {}

    @Override
    public void initializeApplicationModel(ApplicationModel applicationModel) {
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();

        List<DubboLifecycleEventMulticaster> lifecycleEventMulticasters = new ArrayList<>();
        CompositeDubboLifecycleEventMulticaster lifecycleEventMulticaster =
                new CompositeDubboLifecycleEventMulticaster(lifecycleEventMulticasters);

        List<DubboEventMulticaster> eventMulticasters = new ArrayList<>();
        CompositeDubboEventMulticaster dubboEventMulticaster = new CompositeDubboEventMulticaster(eventMulticasters);

        ExtensionLoader<DubboEventMulticaster> extensionLoader =
                applicationModel.getExtensionLoader(DubboEventMulticaster.class);
        if (extensionLoader != null) {
            for (DubboEventMulticaster eventMulticaster : extensionLoader.getActivateExtensions()) {
                eventMulticasters.add(eventMulticaster);
                if (eventMulticaster instanceof DubboLifecycleEventMulticaster) {
                    lifecycleEventMulticasters.add((DubboLifecycleEventMulticaster) eventMulticaster);
                }
            }
        }

        beanFactory.registerBean(lifecycleEventMulticaster);
        beanFactory.registerBean(dubboEventMulticaster);
    }

    @Override
    public void initializeModuleModel(ModuleModel moduleModel) {}
}
