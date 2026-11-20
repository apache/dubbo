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
package org.apache.dubbo.config.deploy;

import org.apache.dubbo.common.deploy.ApplicationDeployListener;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.config.MetricsConfig;
import org.apache.dubbo.metrics.utils.MetricsSupportUtil;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.FrameworkModel;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.dubbo.common.constants.MetricsConstants.PROTOCOL_PROMETHEUS;

class DefaultApplicationDeployerTest {

    @Test
    void isSupportPrometheus() {
        boolean supportPrometheus = MetricsSupportUtil.isSupportPrometheus();
        Assert.assertTrue(supportPrometheus, "MetricsSupportUtil.isSupportPrometheus() should return true");
    }

    @Test
    void isImportPrometheus() {
        MetricsConfig metricsConfig = new MetricsConfig();
        metricsConfig.setProtocol("prometheus");
        boolean importPrometheus =
                PROTOCOL_PROMETHEUS.equals(metricsConfig.getProtocol()) && !MetricsSupportUtil.isSupportPrometheus();
        Assert.assertTrue(!importPrometheus, " should return false");
    }

    /**
     * See <a href="https://github.com/apache/dubbo/issues/14859">#14859</a>.
     * <p>
     * Programmatic {@code ServiceConfig.export()} may invoke
     * {@code ApplicationDeployer.exportMetadataService()} while the application
     * deployer is still in the {@code PENDING} state (e.g. when the user wires
     * Dubbo via XML without any {@code <dubbo:service/>} entry and then exports
     * services manually). The metadata service must still be exported in that
     * case, otherwise instance-level registration is silently skipped.
     */
    @Test
    void exportMetadataServiceShouldFireListenersWhenDeployerIsPending() {
        FrameworkModel frameworkModel = new FrameworkModel();
        try {
            ApplicationModel applicationModel = frameworkModel.newApplication();
            DefaultApplicationDeployer deployer =
                    (DefaultApplicationDeployer) DefaultApplicationDeployer.get(applicationModel);

            AtomicInteger moduleStartedInvocations = new AtomicInteger();
            deployer.addDeployListener(new ApplicationDeployListener() {
                @Override
                public void onInitialize(ApplicationModel scopeModel) {}

                @Override
                public void onStarting(ApplicationModel scopeModel) {}

                @Override
                public void onStarted(ApplicationModel scopeModel) {}

                @Override
                public void onCompletion(ApplicationModel scopeModel) {}

                @Override
                public void onStopping(ApplicationModel scopeModel) {}

                @Override
                public void onStopped(ApplicationModel scopeModel) {}

                @Override
                public void onFailure(ApplicationModel scopeModel, Throwable cause) {}

                @Override
                public void onModuleStarted(ApplicationModel scopeModel) {
                    moduleStartedInvocations.incrementAndGet();
                }
            });

            // Sanity: the deployer is in PENDING state until start() is called.
            Assertions.assertTrue(deployer.isPending());

            deployer.exportMetadataService();

            Assertions.assertEquals(
                    1,
                    moduleStartedInvocations.get(),
                    "ApplicationDeployListener.onModuleStarted should be invoked even when the deployer is in PENDING state");
        } finally {
            frameworkModel.destroy();
        }
    }
}
