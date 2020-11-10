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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
import org.apache.dubbo.registry.client.migration.model.MigrationStep;

import static org.apache.dubbo.common.constants.RegistryConstants.INIT;

@Activate
public class MigrationRuleHandler<T> {
    private static final Logger logger = LoggerFactory.getLogger(MigrationRuleHandler.class);
    private static final String DUBBO_SERVICEDISCOVERY_MIGRATION = "dubbo.application.service-discovery.migration";

    private MigrationClusterInvoker<T> migrationInvoker;
    private MigrationStep currentStep;
    private Float currentThreshold = 0f;
    private MigrationRule rule;
    private URL consumerURL;

    public MigrationRuleHandler(MigrationClusterInvoker<T> invoker, URL url) {
        this.migrationInvoker = invoker;
        this.consumerURL = url;
    }

    public void doMigrate(String rawRule) {
        MigrationStep step = (migrationInvoker instanceof ServiceDiscoveryMigrationInvoker)
                ? MigrationStep.FORCE_APPLICATION
                : MigrationStep.INTERFACE_FIRST;
        Float threshold = 0f;
        if (StringUtils.isEmpty(rawRule)) {
            logger.error("Find empty migration rule, will ignore.");
            return;
        } else if (INIT.equals(rawRule)) {
            step = Enum.valueOf(MigrationStep.class, ConfigurationUtils.getDynamicProperty(DUBBO_SERVICEDISCOVERY_MIGRATION, step.name()));
        } else {
            try {
                rule = MigrationRule.parse(rawRule);
                // FIXME, consumerURL.getHost() might not exactly the ip expected.
                if (CollectionUtils.isEmpty(rule.getTargetIps()) || (rule.getTargetIps() != null && rule.getTargetIps().contains(consumerURL.getHost()))) {
                    setMigrationRule(rule);
                    step = rule.getStep(consumerURL.getServiceKey());
                    threshold = rule.getThreshold(consumerURL.getServiceKey());
                } else {
                    logger.info("Migration rule ignored, rule target ips " + rule.getTargetIps() + " and local ip " + consumerURL.getHost() + " do not match");
                }
            } catch (Exception e) {
                logger.error("Parse migration rule error, will use default step " + step, e);
            }
        }

        if ((currentStep == null || currentStep != step) || (!currentThreshold.equals(threshold))) {
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

    public void setCurrentStepAndThreshold(MigrationStep currentStep, Float currentThreshold) {
        this.currentStep = currentStep;
        this.currentThreshold = currentThreshold;
    }

    public void setMigrationRule(MigrationRule rule) {
        this.migrationInvoker.setMigrationStep(currentStep);
        this.migrationInvoker.setMigrationRule(rule);
    }
}
