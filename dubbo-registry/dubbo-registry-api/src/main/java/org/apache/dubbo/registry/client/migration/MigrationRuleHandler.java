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
package org.apache.dubbo.registry.client.migration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
import org.apache.dubbo.registry.client.migration.model.MigrationStep;

import java.util.Set;

@Activate
public class MigrationRuleHandler<T> {
    private static final Logger logger = LoggerFactory.getLogger(MigrationRuleHandler.class);
    private static final String DUBBO_SERVICEDISCOVERY_MIGRATION = "dubbo.application.service-discovery.migration";

    private MigrationClusterInvoker<T> migrationInvoker;
    private MigrationStep currentStep;
    private Float currentThreshold = 0f;
    private URL consumerURL;

    private final WritableMetadataService writableMetadataService;

    public MigrationRuleHandler(MigrationClusterInvoker<T> invoker, URL url) {
        this.migrationInvoker = invoker;
        this.consumerURL = url;
        this.writableMetadataService = WritableMetadataService.getDefaultExtension();
    }

    public synchronized void doMigrate(MigrationRule rule, boolean isCallback) {
        if (migrationInvoker instanceof ServiceDiscoveryMigrationInvoker) {
            if (!isCallback) {
                initInvoker(MigrationStep.FORCE_APPLICATION, 1.0f);
            } else {
                migrationInvoker.refreshServiceDiscoveryInvokerOnMappingCallback(true);
            }
            return;
        }

        MigrationStep step = MigrationStep.INTERFACE_FIRST;
        Float threshold = -1f;
        if (rule == MigrationRule.INIT) {
            step = Enum.valueOf(MigrationStep.class, ConfigurationUtils.getDynamicProperty(DUBBO_SERVICEDISCOVERY_MIGRATION, step.name()));
        } else {
            try {
                String serviceKey = consumerURL.getDisplayServiceKey();
                Set<String> apps = writableMetadataService.getCachedMapping(consumerURL);
                // FIXME, consumerURL.getHost() might not exactly the ip expected.
                if (CollectionUtils.isNotEmpty(apps)) { //empty only happens when meta server does not work properly
                    if (CollectionUtils.isEmpty(rule.getTargetIps())) {
                        setMigrationRule(rule);
                        step = getMigrationStep(rule, step, serviceKey, apps);
                        threshold = getMigrationThreshold(rule, threshold, serviceKey, apps);
                    } else {
                        if (rule.getTargetIps().contains(consumerURL.getHost())) {
                            setMigrationRule(rule);
                            step = getMigrationStep(rule, step, serviceKey, apps);
                            threshold = getMigrationThreshold(rule, threshold, serviceKey, apps);
                        } else {
                            setMigrationRule(null); // clear previous rule
                            logger.info("New migration rule ignored and previous migration rule cleared, new target ips " + rule.getTargetIps() + " and local ip " + consumerURL.getHost() + " do not match");
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Failed to get step and threshold info from rule: " + rule, e);
            }
        }

        if (!isCallback) {
            initInvoker(step, threshold);
        } else {
            refreshInvoker(step, threshold);
        }
    }

    private void initInvoker(MigrationStep step, Float threshold) {
        if (step == null || threshold == null) {
            throw new IllegalStateException("Step or threshold of migration rule cannot be null");
        }
        if ((currentStep == null || currentStep != step) || !currentThreshold.equals(threshold)) {
            setCurrentStepAndThreshold(step, threshold);
            switch (step) {
                case APPLICATION_FIRST:
                    migrationInvoker.migrateToServiceDiscoveryInvoker(false);
                    break;
                case FORCE_APPLICATION:
                    migrationInvoker.migrateToServiceDiscoveryInvoker(true);
                    break;
                case INTERFACE_FIRST:
                default:
                    migrationInvoker.fallbackToInterfaceInvoker();
            }
        }
    }

    private void refreshInvoker(MigrationStep step, Float threshold) {
        if (step == null || threshold == null) {
            throw new IllegalStateException("Step or threshold of migration rule cannot be null");
        }

        if (step == MigrationStep.APPLICATION_FIRST) {
            migrationInvoker.refreshServiceDiscoveryInvokerOnMappingCallback(false);
        } else if (step == MigrationStep.FORCE_APPLICATION) {
            migrationInvoker.refreshServiceDiscoveryInvokerOnMappingCallback(true);
        }
    }

    public void setMigrationRule(MigrationRule rule) {
        this.migrationInvoker.setMigrationRule(rule);
    }

    private MigrationStep getMigrationStep(MigrationRule rule, MigrationStep step, String serviceKey, Set<String> apps) {
        MigrationStep configuredStep = rule.getStep(serviceKey, apps);
        step = configuredStep == null ? step : configuredStep;
        return step;
    }

    private Float getMigrationThreshold(MigrationRule rule, Float threshold, String serviceKey, Set<String> apps) {
        Float configuredThreshold = rule.getThreshold(serviceKey, apps);
        threshold = configuredThreshold == null ? threshold : configuredThreshold;
        return threshold;
    }

    public void setCurrentStepAndThreshold(MigrationStep currentStep, Float currentThreshold) {
        if (currentThreshold != null) {
            this.currentThreshold = currentThreshold;
        }
        if (currentStep != null) {
            this.currentStep = currentStep;
            this.migrationInvoker.setMigrationStep(currentStep);
        }
    }
}
