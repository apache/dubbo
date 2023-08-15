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
package org.apache.dubbo.config.deploy.lifecycle;

import org.apache.dubbo.common.deploy.DeployState;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.metrics.collector.DefaultMetricsCollector;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.Objects;

@Activate(order = -2000)
public class MetricsCollectorStartLifecycle implements ApplicationLifecycle{

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MetricsCollectorStartLifecycle.class);

    @Override
    public boolean needInitialize(ApplicationContext context) {
        return true;
    }


    @Override
    public void postModuleChanged(ApplicationContext applicationContext, ModuleModel changedModule, DeployState moduleNewState, DeployState applicationOldState, DeployState applicationNewState) {
            if(DeployState.STARTED.equals(applicationNewState) && DeployState.STARTING.equals(applicationOldState)){
                startMetricsCollector(applicationContext.getModel());
            }
            if (logger.isInfoEnabled()) {
                logger.info(applicationContext.getModel().getDesc() + " is ready.");
            }
    }

    private void startMetricsCollector(ApplicationModel applicationModel) {
        DefaultMetricsCollector collector = applicationModel.getBeanFactory().getBean(DefaultMetricsCollector.class);

        if (Objects.nonNull(collector) && collector.isThreadpoolCollectEnabled()) {
            collector.registryDefaultSample();
        }
    }
}
