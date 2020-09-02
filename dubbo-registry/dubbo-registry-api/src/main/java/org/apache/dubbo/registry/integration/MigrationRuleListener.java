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
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationRule;

@Activate
public class MigrationRuleListener<T> {
    private static final Logger logger = LoggerFactory.getLogger(MigrationRuleListener.class);

    private MigrationInvoker<T> migrationInvoker;

    private boolean migrationMultiRegsitry;

    public MigrationRuleListener(MigrationInvoker<T> invoker, boolean migrationMultiRegsitry) {
        this.migrationInvoker = invoker;
        this.migrationMultiRegsitry = migrationMultiRegsitry;
    }

    public void doMigrate(String rawRule) {
        MigrationRule rule = MigrationRule.parse(rawRule);

        migrationInvoker.setMigrationRule(rule);

        if (migrationMultiRegsitry) {
            if (migrationInvoker.isServiceInvoker()) {
                migrationInvoker.refreshServiceDiscoveryInvoker();
            } else {
                migrationInvoker.refreshInterfaceInvoker();
            }
            // TODO 关注下会不会重复添加Listener？？？？
            migrationInvoker.addAddressChangeListener();

        } else {
            switch (rule.getStep()) {
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
}