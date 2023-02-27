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
<<<<<<< HEAD
=======
import org.apache.dubbo.common.config.ConfigurationUtils;
>>>>>>> origin/3.2
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.extension.Activate;
<<<<<<< HEAD
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
=======
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.FrameworkExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
>>>>>>> origin/3.2
import org.apache.dubbo.registry.integration.RegistryProtocol;
import org.apache.dubbo.registry.integration.RegistryProtocolListener;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
<<<<<<< HEAD
import org.apache.dubbo.rpc.cluster.support.migration.MigrationRule;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Optional;
import java.util.Set;

import static org.apache.dubbo.common.constants.RegistryConstants.INIT;

@Activate
public class MigrationRuleListener implements RegistryProtocolListener, ConfigurationListener {
    private static final Logger logger = LoggerFactory.getLogger(MigrationRuleListener.class);

    private Set<MigrationRuleHandler> listeners = new ConcurrentHashSet<>();
    private DynamicConfiguration configuration;

    private volatile String rawRule;

    public MigrationRuleListener() {
        Optional<DynamicConfiguration> optional = ApplicationModel.getEnvironment().getDynamicConfiguration();

        if (optional.isPresent()) {
            this.configuration = optional.get();

            logger.info("Listening for migration rules on dataId-" + MigrationRule.RULE_KEY + " group-" + MigrationRule.DUBBO_SERVICEDISCOVERY_MIGRATION_GROUP);
            configuration.addListener(MigrationRule.RULE_KEY, MigrationRule.DUBBO_SERVICEDISCOVERY_MIGRATION_GROUP, this);

            rawRule = configuration.getConfig(MigrationRule.RULE_KEY, MigrationRule.DUBBO_SERVICEDISCOVERY_MIGRATION_GROUP);
            if (StringUtils.isEmpty(rawRule)) {
                rawRule = INIT;
            }

        } else {
            if (logger.isWarnEnabled()) {
                logger.warn("config center is not configured!");
            }

            rawRule = INIT;
        }

        process(new ConfigChangedEvent(MigrationRule.RULE_KEY, MigrationRule.DUBBO_SERVICEDISCOVERY_MIGRATION_GROUP, rawRule));
=======
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_PROPERTY_TYPE_MISMATCH;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_THREAD_INTERRUPTED_EXCEPTION;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.INTERNAL_ERROR;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_EMPTY_ADDRESS;
import static org.apache.dubbo.common.constants.RegistryConstants.INIT;

/**
 * Listens to {@MigrationRule} from Config Center.
 * <p>
 * - Migration rule is of consumer application scope.
 * - Listener is shared among all invokers (interfaces), it keeps the relation between interface and handler.
 * <p>
 * There are two execution points:
 * - Refer, invoker behaviour is determined with default rule.
 * - Rule change, invoker behaviour is changed according to the newly received rule.
 */
@Activate
public class MigrationRuleListener implements RegistryProtocolListener, ConfigurationListener {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MigrationRuleListener.class);
    private static final String DUBBO_SERVICEDISCOVERY_MIGRATION = "DUBBO_SERVICEDISCOVERY_MIGRATION";
    private static final String MIGRATION_DELAY_KEY = "dubbo.application.migration.delay";
    private static final int MIGRATION_DEFAULT_DELAY_TIME = 60000;
    private String ruleKey;

    protected final ConcurrentMap<MigrationInvoker<?>, MigrationRuleHandler<?>> handlers = new ConcurrentHashMap<>();
    protected final LinkedBlockingQueue<String> ruleQueue = new LinkedBlockingQueue<>();

    private final AtomicBoolean executorSubmit = new AtomicBoolean(false);
    private final ExecutorService ruleManageExecutor = Executors.newFixedThreadPool(1, new NamedThreadFactory("Dubbo-Migration-Listener"));

    protected ScheduledFuture<?> localRuleMigrationFuture;
    protected Future<?> ruleMigrationFuture;

    private DynamicConfiguration configuration;

    private volatile String rawRule;
    private volatile MigrationRule rule;
    private final ModuleModel moduleModel;

    public MigrationRuleListener(ModuleModel moduleModel) {
        this.moduleModel = moduleModel;
        init();
    }

    private void init() {
        this.ruleKey = moduleModel.getApplicationModel().getApplicationName() + ".migration";
        this.configuration = moduleModel.getModelEnvironment().getDynamicConfiguration().orElse(null);

        if (this.configuration != null) {
            logger.info("Listening for migration rules on dataId " + ruleKey + ", group " + DUBBO_SERVICEDISCOVERY_MIGRATION);
            configuration.addListener(ruleKey, DUBBO_SERVICEDISCOVERY_MIGRATION, this);

            String rawRule = configuration.getConfig(ruleKey, DUBBO_SERVICEDISCOVERY_MIGRATION);
            if (StringUtils.isEmpty(rawRule)) {
                rawRule = INIT;
            }
            setRawRule(rawRule);
        } else {
            if (logger.isWarnEnabled()) {
                logger.warn(REGISTRY_EMPTY_ADDRESS, "", "", "Using default configuration rule because config center is not configured!");
            }
            setRawRule(INIT);
        }

        String localRawRule = moduleModel.getModelEnvironment().getLocalMigrationRule();
        if (!StringUtils.isEmpty(localRawRule)) {
            localRuleMigrationFuture = moduleModel.getApplicationModel().getFrameworkModel().getBeanFactory()
                .getBean(FrameworkExecutorRepository.class).getSharedScheduledExecutor()
                .schedule(() -> {
                    if (this.rawRule.equals(INIT)) {
                        this.process(new ConfigChangedEvent(null, null, localRawRule));
                    }
                }, getDelay(), TimeUnit.MILLISECONDS);
        }
    }

    private int getDelay() {
        int delay = MIGRATION_DEFAULT_DELAY_TIME;
        String delayStr = ConfigurationUtils.getProperty(moduleModel, MIGRATION_DELAY_KEY);
        if (StringUtils.isEmpty(delayStr)) {
            return delay;
        }

        try {
            delay = Integer.parseInt(delayStr);
        } catch (Exception e) {
            logger.warn(COMMON_PROPERTY_TYPE_MISMATCH, "", "", "Invalid migration delay param " + delayStr);
        }
        return delay;
>>>>>>> origin/3.2
    }

    @Override
    public synchronized void process(ConfigChangedEvent event) {
<<<<<<< HEAD
        rawRule = event.getContent();
        if (StringUtils.isEmpty(rawRule)) {
            logger.warn("Received empty migration rule, will ignore.");
            return;
        }

        logger.info("Using the following migration rule to migrate:");
        logger.info(rawRule);

        if (CollectionUtils.isNotEmpty(listeners)) {
            listeners.forEach(listener -> listener.doMigrate(rawRule));
        }
    }

    @Override
    public synchronized void onExport(RegistryProtocol registryProtocol, Exporter<?> exporter) {
=======
        String rawRule = event.getContent();
        if (StringUtils.isEmpty(rawRule)) {
            // fail back to startup status
            rawRule = INIT;
            //logger.warn(COMMON_PROPERTY_TYPE_MISMATCH, "", "", "Received empty migration rule, will ignore.");
        }
        try {
            ruleQueue.put(rawRule);
        } catch (InterruptedException e) {
            logger.error(COMMON_THREAD_INTERRUPTED_EXCEPTION, "", "", "Put rawRule to rule management queue failed. rawRule: " + rawRule, e);
        }

        if (executorSubmit.compareAndSet(false, true)) {
            ruleMigrationFuture = ruleManageExecutor.submit(() -> {
                while (true) {
                    String rule = "";
                    try {
                        rule = ruleQueue.take();
                        if (StringUtils.isEmpty(rule)) {
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        logger.error(COMMON_THREAD_INTERRUPTED_EXCEPTION, "", "", "Poll Rule from config center failed.", e);
                    }
                    if (StringUtils.isEmpty(rule)) {
                        continue;
                    }
                    if (Objects.equals(this.rawRule, rule)) {
                        logger.info("Ignore duplicated rule");
                        continue;
                    }

                    logger.info("Using the following migration rule to migrate:");
                    logger.info(rule);

                    setRawRule(rule);

                    if (CollectionUtils.isEmptyMap(handlers)) {
                        continue;
                    }

                    ExecutorService executorService = null;
                    try {
                        executorService = Executors.newFixedThreadPool(Math.min(handlers.size(), 100), new NamedThreadFactory("Dubbo-Invoker-Migrate"));
                        List<Future<?>> migrationFutures = new ArrayList<>(handlers.size());
                        for (MigrationRuleHandler<?> handler : handlers.values()) {
                            Future<?> future = executorService.submit(() -> handler.doMigrate(this.rule));
                            migrationFutures.add(future);
                        }

                        for (Future<?> future : migrationFutures) {
                            try {
                                future.get();
                            } catch (InterruptedException ie) {
                                logger.warn(INTERNAL_ERROR, "unknown error in registry module", "", "Interrupted while waiting for migration async task to finish.");
                            } catch (ExecutionException ee) {
                                logger.error(INTERNAL_ERROR, "unknown error in registry module", "", "Migration async task failed.", ee.getCause());
                            }
                        }
                    } catch (Throwable t) {
                        logger.error(INTERNAL_ERROR, "unknown error in registry module", "", "Error occurred when migration.", t);
                    } finally {
                        if (executorService != null) {
                            executorService.shutdown();
                        }
                    }
                }
            });
        }
    }

    public void setRawRule(String rawRule) {
        this.rawRule = rawRule;
        this.rule = parseRule(this.rawRule);
    }

    private MigrationRule parseRule(String rawRule) {
        MigrationRule tmpRule = rule == null ? MigrationRule.getInitRule() : rule;
        if (INIT.equals(rawRule)) {
            tmpRule = MigrationRule.getInitRule();
        } else {
            try {
                tmpRule = MigrationRule.parse(rawRule);
            } catch (Exception e) {
                logger.error(COMMON_PROPERTY_TYPE_MISMATCH, "", "", "Failed to parse migration rule...", e);
            }
        }
        return tmpRule;
    }

    @Override
    public void onExport(RegistryProtocol registryProtocol, Exporter<?> exporter) {
>>>>>>> origin/3.2

    }

    @Override
<<<<<<< HEAD
    public synchronized void onRefer(RegistryProtocol registryProtocol, ClusterInvoker<?> invoker, URL url) {
        MigrationInvoker<?> migrationInvoker = (MigrationInvoker<?>) invoker;

        MigrationRuleHandler<?> migrationListener = new MigrationRuleHandler<>(migrationInvoker);
        listeners.add(migrationListener);

        migrationListener.doMigrate(rawRule);
=======
    public void onRefer(RegistryProtocol registryProtocol, ClusterInvoker<?> invoker, URL consumerUrl, URL registryURL) {
        MigrationRuleHandler<?> migrationRuleHandler = ConcurrentHashMapUtils.computeIfAbsent(handlers, (MigrationInvoker<?>) invoker, _key -> {
            ((MigrationInvoker<?>) invoker).setMigrationRuleListener(this);
            return new MigrationRuleHandler<>((MigrationInvoker<?>) invoker, consumerUrl);
        });

        migrationRuleHandler.doMigrate(rule);
>>>>>>> origin/3.2
    }

    @Override
    public void onDestroy() {
<<<<<<< HEAD
        if (null != configuration) {
            configuration.removeListener(MigrationRule.RULE_KEY, MigrationRule.DUBBO_SERVICEDISCOVERY_MIGRATION_GROUP, this);
        }
=======
        if (configuration != null) {
            configuration.removeListener(ruleKey, DUBBO_SERVICEDISCOVERY_MIGRATION, this);
        }
        if (ruleMigrationFuture != null) {
            ruleMigrationFuture.cancel(true);
        }
        if (localRuleMigrationFuture != null) {
            localRuleMigrationFuture.cancel(true);
        }
        ruleManageExecutor.shutdown();
        ruleQueue.clear();
    }

    public Map<MigrationInvoker<?>, MigrationRuleHandler<?>> getHandlers() {
        return handlers;
    }

    protected void removeMigrationInvoker(MigrationInvoker<?> migrationInvoker) {
        handlers.remove(migrationInvoker);
    }

    public MigrationRule getRule() {
        return rule;
>>>>>>> origin/3.2
    }
}
