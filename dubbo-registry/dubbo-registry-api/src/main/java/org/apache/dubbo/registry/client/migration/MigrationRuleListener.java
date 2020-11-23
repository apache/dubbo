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
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.integration.RegistryProtocol;
import org.apache.dubbo.registry.integration.RegistryProtocolListener;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Set;

import static org.apache.dubbo.common.constants.RegistryConstants.INIT;

@Activate
public class MigrationRuleListener implements RegistryProtocolListener, ConfigurationListener {
    private static final Logger logger = LoggerFactory.getLogger(MigrationRuleListener.class);
    private static final String RULE_KEY = ApplicationModel.getName() + ".migration";
    private static final String DUBBO_SERVICEDISCOVERY_MIGRATION = "DUBBO_SERVICEDISCOVERY_MIGRATION";

    private Set<MigrationRuleHandler> handlers = new ConcurrentHashSet<>();
    private DynamicConfiguration configuration;

    private volatile String rawRule;

    public MigrationRuleListener() {
        this.configuration = ApplicationModel.getEnvironment().getDynamicConfiguration().orElse(null);

        logger.info("Listening for migration rules on dataId-" + RULE_KEY + " group-" + DUBBO_SERVICEDISCOVERY_MIGRATION);
        configuration.addListener(RULE_KEY, DUBBO_SERVICEDISCOVERY_MIGRATION, this);

        String rawRule = configuration.getConfig(RULE_KEY, DUBBO_SERVICEDISCOVERY_MIGRATION);
        if (StringUtils.isEmpty(rawRule)) {
            rawRule = INIT;
        }
        process(new ConfigChangedEvent(RULE_KEY, DUBBO_SERVICEDISCOVERY_MIGRATION, rawRule));
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

        if (CollectionUtils.isNotEmpty(handlers)) {
            handlers.forEach(listener -> listener.doMigrate(rawRule));
        }
    }

    @Override
    public synchronized void onExport(RegistryProtocol registryProtocol, Exporter<?> exporter) {

    }

    @Override
    public synchronized void onRefer(RegistryProtocol registryProtocol, ClusterInvoker<?> invoker, URL url) {
        MigrationInvoker<?> migrationInvoker = (MigrationInvoker<?>) invoker;

        MigrationRuleHandler<?> migrationRuleHandler = new MigrationRuleHandler<>(migrationInvoker, url);
        handlers.add(migrationRuleHandler);

        migrationRuleHandler.doMigrate(rawRule);
    }

    @Override
    public void onDestroy() {
        configuration.removeListener(RULE_KEY, this);
    }
}
