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
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.WritableMetadataService;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
import org.apache.dubbo.registry.integration.RegistryProtocol;
import org.apache.dubbo.registry.integration.RegistryProtocolListener;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.apache.dubbo.common.constants.CommonConstants.MAPPING_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.INIT;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDED_BY;
import static org.apache.dubbo.common.constants.RegistryConstants.SUBSCRIBED_SERVICE_NAMES_KEY;
import static org.apache.dubbo.common.utils.CollectionUtils.isEmpty;
import static org.apache.dubbo.common.utils.StringUtils.isBlank;

@Activate
public class MigrationRuleListener implements RegistryProtocolListener, ConfigurationListener {
    private static final Logger logger = LoggerFactory.getLogger(MigrationRuleListener.class);
    private static final String RULE_KEY = ApplicationModel.getName() + ".migration";
    private static final String DUBBO_SERVICEDISCOVERY_MIGRATION = "DUBBO_SERVICEDISCOVERY_MIGRATION";

    private Map<String, MigrationRuleHandler> handlers = new ConcurrentHashMap<>();
    private DynamicConfiguration configuration;

    private volatile String rawRule;
    private volatile MigrationRule rule;

    public MigrationRuleListener() {
        this.configuration = ApplicationModel.getEnvironment().getDynamicConfiguration().orElse(null);

        String localRawRule = ApplicationModel.getEnvironment().getLocalMigrationRule();
        String defaultRawRule = StringUtils.isEmpty(localRawRule) ? INIT : localRawRule;

        if (this.configuration != null) {
            logger.info("Listening for migration rules on dataId " + RULE_KEY + ", group " + DUBBO_SERVICEDISCOVERY_MIGRATION);
            configuration.addListener(RULE_KEY, DUBBO_SERVICEDISCOVERY_MIGRATION, this);

            String rawRule = configuration.getConfig(RULE_KEY, DUBBO_SERVICEDISCOVERY_MIGRATION);
            if (StringUtils.isEmpty(rawRule)) {
                rawRule = defaultRawRule;
            }
            this.rawRule = rawRule;
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("Using default configuration rule because config center is not configured!");
            }
            rawRule = defaultRawRule;
        }
//        process(new ConfigChangedEvent(RULE_KEY, DUBBO_SERVICEDISCOVERY_MIGRATION, rawRule));
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
            handlers.forEach((_key, handler) -> handler.doMigrate(rule, false));
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
        MigrationRuleHandler<?> migrationRuleHandler = handlers.computeIfAbsent(consumerUrl.getServiceKey() + consumerUrl.getParameter(TIMESTAMP_KEY), _key -> {
            return new MigrationRuleHandler<>((MigrationInvoker<?>)invoker, consumerUrl);
        });

        try {
            Set<String> subscribedServices = getServices(registryURL, consumerUrl, migrationRuleHandler);
            WritableMetadataService.getDefaultExtension().putCachedMapping(ServiceNameMapping.buildMappingKey(consumerUrl), subscribedServices);
        } catch (Exception e) {
            logger.warn("Cannot find app mapping for service " + consumerUrl.getServiceInterface() + ", will not migrate.", e);
        }

        rule = parseRule(rawRule);

        migrationRuleHandler.doMigrate(rule, false);
    }

    @Override
    public void onDestroy() {
        if (configuration != null)
            configuration.removeListener(RULE_KEY, this);
    }

    /**
     * 1.developer explicitly specifies the application name this interface belongs to
     * 2.check Interface-App mapping
     * 3.use the services specified in registry url.
     *
     * @param subscribedURL
     * @return
     */
    protected Set<String> getServices(URL registryURL, URL subscribedURL, MigrationRuleHandler handler) {
        Set<String> subscribedServices = new TreeSet<>();
        Set<String> globalConfiguredSubscribingServices = parseServices(registryURL.getParameter(SUBSCRIBED_SERVICE_NAMES_KEY));

        String serviceNames = subscribedURL.getParameter(PROVIDED_BY);
        if (StringUtils.isNotEmpty(serviceNames)) {
            logger.info(subscribedURL.getServiceInterface() + " mapping to " + serviceNames + " instructed by provided-by set by user.");
            subscribedServices.addAll(parseServices(serviceNames));
        }

        if (isEmpty(subscribedServices)) {
            Set<String> mappedServices = findMappedServices(registryURL, subscribedURL, new DefaultMappingListener(subscribedURL, subscribedServices, handler));
            logger.info(subscribedURL.getServiceInterface() + " mapping to " + mappedServices + " instructed by remote metadata center.");
            subscribedServices.addAll(mappedServices);
            if (isEmpty(subscribedServices)) {
                logger.info(subscribedURL.getServiceInterface() + " mapping to " + globalConfiguredSubscribingServices + " by default.");
                subscribedServices.addAll(globalConfiguredSubscribingServices);
            }
        }
        return subscribedServices;
    }

    protected Set<String> findMappedServices(URL registryURL, URL subscribedURL, MappingListener listener) {
        Set<String> result = new LinkedHashSet<>();
        ServiceNameMapping serviceNameMapping = ServiceNameMapping.getExtension(registryURL.getParameter(MAPPING_KEY));
        result.addAll(serviceNameMapping.getAndListen(subscribedURL, listener));
        result.addAll(serviceNameMapping.getAndListenWithNewStore(subscribedURL, listener));
        return result;
    }

    public static Set<String> parseServices(String literalServices) {
        return isBlank(literalServices) ? emptySet() :
                unmodifiableSet(of(literalServices.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotEmpty)
                        .collect(toSet()));
    }

    private class DefaultMappingListener implements MappingListener {
        private final Logger logger = LoggerFactory.getLogger(DefaultMappingListener.class);
        private URL url;
        private Set<String> oldApps;
        private MigrationRuleHandler handler;

        public DefaultMappingListener(URL subscribedURL, Set<String> serviceNames, MigrationRuleHandler handler) {
            this.url = subscribedURL;
            this.oldApps = serviceNames;
            this.handler = handler;
        }

        @Override
        public void onEvent(MappingChangedEvent event) {
            if(logger.isDebugEnabled()) {
                logger.debug("Received mapping notification from meta server, " + event);
            }
            Set<String> newApps = event.getApps();
            Set<String> tempOldApps = oldApps;
            oldApps = newApps;

            if (CollectionUtils.isEmpty(newApps)) {
                return;
            }

            if (CollectionUtils.isEmpty(tempOldApps) && newApps.size() > 0) {
                WritableMetadataService.getDefaultExtension().putCachedMapping(ServiceNameMapping.buildMappingKey(url), newApps);
                handler.doMigrate(rule, true);
                return;
            }

            for (String newAppName : newApps) {
                if (!tempOldApps.contains(newAppName)) {
                    WritableMetadataService.getDefaultExtension().putCachedMapping(ServiceNameMapping.buildMappingKey(url), newApps);
                    handler.doMigrate(rule, true);
                    return;
                }
            }
        }
    }
}
