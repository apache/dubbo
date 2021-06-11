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
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
import org.apache.dubbo.registry.integration.RegistryProtocol;
import org.apache.dubbo.registry.integration.RegistryProtocolListener;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.RegistryConstants.INIT;

@Activate
public class MigrationRuleListener implements RegistryProtocolListener, ConfigurationListener {
    private static final Logger logger = LoggerFactory.getLogger(MigrationRuleListener.class);
    private static final String DUBBO_SERVICEDISCOVERY_MIGRATION = "DUBBO_SERVICEDISCOVERY_MIGRATION";
    private static final String MIGRATION_DELAY_KEY = "dubbo.application.migration.delay";
    private final String RULE_KEY = ApplicationModel.getName() + ".migration";

    private final Map<MigrationInvoker, MigrationRuleHandler> handlers = new ConcurrentHashMap<>();
    private DynamicConfiguration configuration;

    private volatile String rawRule;
    private volatile MigrationRule rule;

    public MigrationRuleListener() {
        this.configuration = ApplicationModel.getEnvironment().getDynamicConfiguration().orElse(null);

        if (this.configuration != null) {
            logger.info("Listening for migration rules on dataId " + RULE_KEY + ", group " + DUBBO_SERVICEDISCOVERY_MIGRATION);
            configuration.addListener(RULE_KEY, DUBBO_SERVICEDISCOVERY_MIGRATION, this);

            String rawRule = configuration.getConfig(RULE_KEY, DUBBO_SERVICEDISCOVERY_MIGRATION);
            if (StringUtils.isEmpty(rawRule)) {
                rawRule = INIT;
            }
            this.rawRule = rawRule;
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Using default configuration rule because config center is not configured!");
            }
            rawRule = INIT;
        }

        String localRawRule = ApplicationModel.getEnvironment().getLocalMigrationRule();
        if (!StringUtils.isEmpty(localRawRule)) {
            Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DubboMigrationRuleDelayWorker", true))
                .schedule(() -> {
                    if (this.rawRule.equals(INIT)) {
                        this.process(new ConfigChangedEvent(null, null, localRawRule));
                    }
                }, getDelay(), TimeUnit.MILLISECONDS);
        }
    }

    private int getDelay() {
        int delay = 60000;
        String delayStr = ConfigurationUtils.getProperty(MIGRATION_DELAY_KEY);
        if (StringUtils.isEmpty(delayStr)) {
            return delay;
        }

        try {
            delay = Integer.parseInt(delayStr);
        } catch (Exception e) {
            logger.warn("Invalid migration delay param " + delayStr);
        }
        return delay;
    }

    @Override
    public synchronized void process(ConfigChangedEvent event) {
        rawRule = event.getContent();
        if (StringUtils.isEmpty(rawRule)) {
            logger.warn("Received empty migration rule, will ignore.");
            return;
        }

        logger.info("Using the following migration rule to migrate:");
        logger.info(rawRule);

        rule = parseRule(rawRule);

        if (CollectionUtils.isNotEmptyMap(handlers)) {
            handlers.forEach((_key, handler) -> handler.doMigrate(rule));
        }
    }

    private MigrationRule parseRule(String rawRule) {
        MigrationRule tmpRule = rule;
        if (INIT.equals(rawRule)) {
            tmpRule = MigrationRule.INIT;
        } else {
            try {
                tmpRule = MigrationRule.parse(rawRule);
            } catch (Exception e) {
                logger.error("Failed to parse migration rule...", e);
            }
        }
        return tmpRule;
    }

    @Override
    public synchronized void onExport(RegistryProtocol registryProtocol, Exporter<?> exporter) {

    }

    @Override
    public synchronized void onRefer(RegistryProtocol registryProtocol, ClusterInvoker<?> invoker, URL consumerUrl, URL registryURL) {
        MigrationRuleHandler<?> migrationRuleHandler = handlers.computeIfAbsent((MigrationInvoker<?>) invoker, _key -> {
            ((MigrationInvoker<?>) invoker).setMigrationRuleListener(this);
            return new MigrationRuleHandler<>((MigrationInvoker<?>) invoker, consumerUrl);
        });

        rule = parseRule(rawRule);

        migrationRuleHandler.doMigrate(rule);
    }

    @Override
    public void onDestroy() {
        if (configuration != null) {
            configuration.removeListener(RULE_KEY, this);
        }
    }

    public Map<MigrationInvoker, MigrationRuleHandler> getHandlers() {
        return handlers;
    }

    protected void removeMigrationInvoker(MigrationInvoker<?> migrationInvoker) {
        handlers.remove(migrationInvoker);
    }
}
