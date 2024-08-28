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
package org.apache.dubbo.metrics;

import org.apache.dubbo.common.Ordered;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.event.DubboEventBus;
import org.apache.dubbo.common.event.DubboListener;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.metrics.collector.MetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.model.ScopeModelInitializer;

import java.util.List;

public class MetricsScopeModelInitializer implements ScopeModelInitializer, Ordered {

    @Override
    public void initializeFrameworkModel(FrameworkModel frameworkModel) {}

    @Override
    public void initializeApplicationModel(ApplicationModel applicationModel) {
        ScopeBeanFactory beanFactory = applicationModel.getBeanFactory();
        ExtensionLoader<MetricsCollector> extensionLoader = applicationModel.getExtensionLoader(MetricsCollector.class);
        if (extensionLoader != null) {
            List<MetricsCollector> customizeCollectors = extensionLoader.getActivateExtensions();
            for (MetricsCollector customizeCollector : customizeCollectors) {
                beanFactory.registerBean(customizeCollector);
            }
        }
        MetricsServiceInitializer metricsServiceInitializer = new MetricsServiceInitializer(applicationModel);
        for (DubboListener<?> dubboListener : metricsServiceInitializer.getDubboListeners()) {
            DubboEventBus.addListener(applicationModel, dubboListener);
        }
    }

    @Override
    public void initializeModuleModel(ModuleModel moduleModel) {}

    @Override
    public int getOrder() {
        return 1;
    }
}
