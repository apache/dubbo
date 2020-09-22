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

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
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

    public MigrationRuleHandler(MigrationClusterInvoker<T> invoker) {
        this.migrationInvoker = invoker;
    }

    public void doMigrate(String rawRule) {
        MigrationStep step = (migrationInvoker instanceof ServiceDiscoveryMigrationInvoker)
                ? MigrationStep.FORCE_APPLICATION
                : MigrationStep.INTERFACE_FIRST;
        if (StringUtils.isEmpty(rawRule)) {
            logger.error("Find empty migration rule, will ignore.");
            return;
        } else if (INIT.equals(rawRule)) {
            step = Enum.valueOf(MigrationStep.class, ConfigurationUtils.getDynamicProperty(DUBBO_SERVICEDISCOVERY_MIGRATION, step.name()));
        } else {
            MigrationRule rule = MigrationRule.parse(rawRule);
            step = rule.getStep();
        }

        if (currentStep == null || currentStep != step) {
            setCurrentStep(step);
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

    public void setCurrentStep(MigrationStep currentStep) {
        this.currentStep = currentStep;
        this.migrationInvoker.setMigrationStep(currentStep);
    }
}
