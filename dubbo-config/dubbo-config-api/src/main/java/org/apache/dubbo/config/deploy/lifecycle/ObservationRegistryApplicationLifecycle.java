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

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.TracingConfig;
import org.apache.dubbo.config.deploy.context.ApplicationContext;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.tracing.DubboObservationRegistry;
import org.apache.dubbo.tracing.utils.ObservationSupportUtil;

import java.util.Optional;

/**
 * Tracing lifecycle
 */
@Activate(order = -800)
public class ObservationRegistryApplicationLifecycle implements ApplicationLifecycle {

    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(ObservationRegistryApplicationLifecycle.class);

    @Override
    public void initialize(ApplicationContext applicationContext) {
        initObservationRegistry(applicationContext);
    }

    private void initObservationRegistry(ApplicationContext applicationContext) {
        ApplicationModel applicationModel = applicationContext.getModel();

        if (!ObservationSupportUtil.isSupportObservation()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not found micrometer-observation or plz check the version of micrometer-observation version if already introduced, need > 1.10.0");
            }
            return;
        }
        if (!ObservationSupportUtil.isSupportTracing()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Not found micrometer-tracing dependency, skip init ObservationRegistry.");
            }
            return;
        }

        Optional<TracingConfig> configManager = applicationModel.getApplicationConfigManager().getTracing();
        boolean needInitialize = configManager.isPresent() && configManager.get().getEnabled();

        if (needInitialize) {
            DubboObservationRegistry dubboObservationRegistry = new DubboObservationRegistry(applicationModel, configManager.get());
            dubboObservationRegistry.initObservationRegistry();
        }
    }

    @Override
    public boolean needInitialize(ApplicationContext context) {
        return true;
    }
}
