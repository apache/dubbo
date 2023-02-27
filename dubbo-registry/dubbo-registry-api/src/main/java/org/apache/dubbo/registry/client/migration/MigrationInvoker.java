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
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.Registry;
=======
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.status.reporter.FrameworkStatusReportService;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
import org.apache.dubbo.registry.client.migration.model.MigrationStep;
>>>>>>> origin/3.2
import org.apache.dubbo.registry.integration.DynamicDirectory;
import org.apache.dubbo.registry.integration.RegistryProtocol;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
<<<<<<< HEAD
import org.apache.dubbo.rpc.cluster.support.migration.MigrationClusterInvoker;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationRule;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

public class MigrationInvoker<T> implements MigrationClusterInvoker<T> {
    private Logger logger = LoggerFactory.getLogger(MigrationInvoker.class);

    private URL url;
    private URL consumerUrl;
    private Cluster cluster;
    private Registry registry;
    private Class<T> type;
    private RegistryProtocol registryProtocol;
=======
import org.apache.dubbo.rpc.model.ConsumerModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_FAILED_NOTIFY_EVENT;
import static org.apache.dubbo.registry.client.migration.model.MigrationStep.APPLICATION_FIRST;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;

public class MigrationInvoker<T> implements MigrationClusterInvoker<T> {
    private final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(MigrationInvoker.class);

    private URL url;
    private final URL consumerUrl;
    private final Cluster cluster;
    private final Registry registry;
    private final Class<T> type;
    private final RegistryProtocol registryProtocol;
    private MigrationRuleListener migrationRuleListener;
    private final ConsumerModel consumerModel;
    private final FrameworkStatusReportService reportService;
>>>>>>> origin/3.2

    private volatile ClusterInvoker<T> invoker;
    private volatile ClusterInvoker<T> serviceDiscoveryInvoker;
    private volatile ClusterInvoker<T> currentAvailableInvoker;
<<<<<<< HEAD

    private MigrationRule rule;

    private boolean migrationMultiRegistry;
=======
    private volatile MigrationStep step;
    private volatile MigrationRule rule;
    private volatile int promotion = 100;
>>>>>>> origin/3.2

    public MigrationInvoker(RegistryProtocol registryProtocol,
                            Cluster cluster,
                            Registry registry,
                            Class<T> type,
                            URL url,
                            URL consumerUrl) {
        this(null, null, registryProtocol, cluster, registry, type, url, consumerUrl);
    }

<<<<<<< HEAD
=======
    @SuppressWarnings("unchecked")
>>>>>>> origin/3.2
    public MigrationInvoker(ClusterInvoker<T> invoker,
                            ClusterInvoker<T> serviceDiscoveryInvoker,
                            RegistryProtocol registryProtocol,
                            Cluster cluster,
                            Registry registry,
                            Class<T> type,
                            URL url,
                            URL consumerUrl) {
        this.invoker = invoker;
        this.serviceDiscoveryInvoker = serviceDiscoveryInvoker;
        this.registryProtocol = registryProtocol;
        this.cluster = cluster;
        this.registry = registry;
        this.type = type;
        this.url = url;
        this.consumerUrl = consumerUrl;
<<<<<<< HEAD
        this.migrationMultiRegistry = url.getParameter(RegistryConstants.MIGRATION_MULTI_REGISTRY, false);
=======
        this.consumerModel = (ConsumerModel) consumerUrl.getServiceModel();
        this.reportService = consumerUrl.getOrDefaultApplicationModel().getBeanFactory().getBean(FrameworkStatusReportService.class);

        if (consumerModel != null) {
            Object object = consumerModel.getServiceMetadata().getAttribute(CommonConstants.CURRENT_CLUSTER_INVOKER_KEY);
            Map<Registry, MigrationInvoker<?>> invokerMap;
            if (object instanceof Map) {
                invokerMap = (Map<Registry, MigrationInvoker<?>>) object;
            } else {
                invokerMap = new ConcurrentHashMap<>();
            }
            invokerMap.put(registry, this);
            consumerModel.getServiceMetadata().addAttribute(CommonConstants.CURRENT_CLUSTER_INVOKER_KEY, invokerMap);
        }
>>>>>>> origin/3.2
    }

    public ClusterInvoker<T> getInvoker() {
        return invoker;
    }

    public void setInvoker(ClusterInvoker<T> invoker) {
        this.invoker = invoker;
    }

    public ClusterInvoker<T> getServiceDiscoveryInvoker() {
        return serviceDiscoveryInvoker;
    }

    public void setServiceDiscoveryInvoker(ClusterInvoker<T> serviceDiscoveryInvoker) {
        this.serviceDiscoveryInvoker = serviceDiscoveryInvoker;
    }

<<<<<<< HEAD
    @Override
    public Class<T> getInterface() {
        return type;
    }

    @Override
    public synchronized void migrateToServiceDiscoveryInvoker(boolean forceMigrate) {
        if (!forceMigrate) {
            refreshServiceDiscoveryInvoker();
            refreshInterfaceInvoker();
            setListener(invoker, () -> {
                this.compareAddresses(serviceDiscoveryInvoker, invoker);
            });
            setListener(serviceDiscoveryInvoker, () -> {
                this.compareAddresses(serviceDiscoveryInvoker, invoker);
            });
        } else {
            refreshServiceDiscoveryInvoker();
            setListener(serviceDiscoveryInvoker, () -> {
                this.destroyInterfaceInvoker(this.invoker);
            });
        }
=======
    public ClusterInvoker<T> getCurrentAvailableInvoker() {
        return currentAvailableInvoker;
    }

    @Override
    public Class<T> getInterface() {
        return type;
>>>>>>> origin/3.2
    }

    @Override
    public void reRefer(URL newSubscribeUrl) {
        // update url to prepare for migration refresh
        this.url = url.addParameter(REFER_KEY, StringUtils.toQueryString(newSubscribeUrl.getParameters()));

        // re-subscribe immediately
        if (invoker != null && !invoker.isDestroyed()) {
            doReSubscribe(invoker, newSubscribeUrl);
        }
        if (serviceDiscoveryInvoker != null && !serviceDiscoveryInvoker.isDestroyed()) {
            doReSubscribe(serviceDiscoveryInvoker, newSubscribeUrl);
        }
    }

    private void doReSubscribe(ClusterInvoker<T> invoker, URL newSubscribeUrl) {
        DynamicDirectory<T> directory = (DynamicDirectory<T>) invoker.getDirectory();
        URL oldSubscribeUrl = directory.getRegisteredConsumerUrl();
        Registry registry = directory.getRegistry();
        registry.unregister(directory.getRegisteredConsumerUrl());
        directory.unSubscribe(RegistryProtocol.toSubscribeUrl(oldSubscribeUrl));
        if (directory.isShouldRegister()) {
            registry.register(directory.getRegisteredConsumerUrl());
            directory.setRegisteredConsumerUrl(newSubscribeUrl);
        }
        directory.buildRouterChain(newSubscribeUrl);
        directory.subscribe(RegistryProtocol.toSubscribeUrl(newSubscribeUrl));
    }

    @Override
<<<<<<< HEAD
    public synchronized void fallbackToInterfaceInvoker() {
        refreshInterfaceInvoker();
        setListener(invoker, () -> {
            this.destroyServiceDiscoveryInvoker(this.serviceDiscoveryInvoker);
        });
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        if (!checkInvokerAvailable(serviceDiscoveryInvoker)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using interface addresses to handle invocation, interface " + type.getName() + ", total address size " + (invoker.getDirectory().getAllInvokers() == null ? "is null" : invoker.getDirectory().getAllInvokers().size()));
            }
            return invoker.invoke(invocation);
        }

        if (!checkInvokerAvailable(invoker)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Using instance addresses to handle invocation, interface " + type.getName() + ", total address size " + (serviceDiscoveryInvoker.getDirectory().getAllInvokers() == null ? " is null " : serviceDiscoveryInvoker.getDirectory().getAllInvokers().size()));
            }
            return serviceDiscoveryInvoker.invoke(invocation);
=======
    public boolean migrateToForceInterfaceInvoker(MigrationRule newRule) {
        CountDownLatch latch = new CountDownLatch(1);
        refreshInterfaceInvoker(latch);

        if (serviceDiscoveryInvoker == null) {
            // serviceDiscoveryInvoker is absent, ignore threshold check
            this.currentAvailableInvoker = invoker;
            return true;
        }

        // wait and compare threshold
        waitAddressNotify(newRule, latch);

        if (newRule.getForce(consumerUrl)) {
            // force migrate, ignore threshold check
            this.currentAvailableInvoker = invoker;
            this.destroyServiceDiscoveryInvoker();
            return true;
        }

        Set<MigrationAddressComparator> detectors = ScopeModelUtil.getApplicationModel(consumerUrl == null ? null : consumerUrl.getScopeModel())
            .getExtensionLoader(MigrationAddressComparator.class).getSupportedExtensionInstances();
        if (CollectionUtils.isNotEmpty(detectors)) {
            if (detectors.stream().allMatch(comparator -> comparator.shouldMigrate(invoker, serviceDiscoveryInvoker, newRule))) {
                this.currentAvailableInvoker = invoker;
                this.destroyServiceDiscoveryInvoker();
                return true;
            }
        }

        // compare failed, will not change state
        if (step == MigrationStep.FORCE_APPLICATION) {
            destroyInterfaceInvoker();
        }
        return false;
    }

    @Override
    public boolean migrateToForceApplicationInvoker(MigrationRule newRule) {
        CountDownLatch latch = new CountDownLatch(1);
        refreshServiceDiscoveryInvoker(latch);

        if (invoker == null) {
            // invoker is absent, ignore threshold check
            this.currentAvailableInvoker = serviceDiscoveryInvoker;
            return true;
        }

        // wait and compare threshold
        waitAddressNotify(newRule, latch);

        if (newRule.getForce(consumerUrl)) {
            // force migrate, ignore threshold check
            this.currentAvailableInvoker = serviceDiscoveryInvoker;
            this.destroyInterfaceInvoker();
            return true;
        }

        Set<MigrationAddressComparator> detectors = ScopeModelUtil.getApplicationModel(consumerUrl == null ? null : consumerUrl.getScopeModel())
            .getExtensionLoader(MigrationAddressComparator.class).getSupportedExtensionInstances();
        if (CollectionUtils.isNotEmpty(detectors)) {
            if (detectors.stream().allMatch(comparator -> comparator.shouldMigrate(serviceDiscoveryInvoker, invoker, newRule))) {
                this.currentAvailableInvoker = serviceDiscoveryInvoker;
                this.destroyInterfaceInvoker();
                return true;
            }
        }

        // compare failed, will not change state
        if (step == MigrationStep.FORCE_INTERFACE) {
            destroyServiceDiscoveryInvoker();
        }
        return false;
    }

    @Override
    public void migrateToApplicationFirstInvoker(MigrationRule newRule) {
        CountDownLatch latch = new CountDownLatch(0);
        refreshInterfaceInvoker(latch);
        refreshServiceDiscoveryInvoker(latch);

        // directly calculate preferred invoker, will not wait until address notify
        // calculation will re-occurred when address notify later
        calcPreferredInvoker(newRule);
    }

    @SuppressWarnings("all")
    private void waitAddressNotify(MigrationRule newRule, CountDownLatch latch) {
        // wait and compare threshold
        int delay = newRule.getDelay(consumerUrl);
        if (delay > 0) {
            try {
                Thread.sleep(delay * 1000L);
            } catch (InterruptedException e) {
                logger.error(REGISTRY_FAILED_NOTIFY_EVENT, "", "", "Interrupted when waiting for address notify!" + e);
            }
        } else {
            // do not wait address notify by default
            delay = 0;
        }
        try {
            latch.await(delay, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(REGISTRY_FAILED_NOTIFY_EVENT, "", "", "Interrupted when waiting for address notify!" + e);
        }
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        if (currentAvailableInvoker != null) {
            if (step == APPLICATION_FIRST) {
                // call ratio calculation based on random value
                if (promotion < 100 && ThreadLocalRandom.current().nextDouble(100) > promotion) {
                    // fall back to interface mode
                    return invoker.invoke(invocation);
                }
                // check if invoker available for each time
                return decideInvoker().invoke(invocation);
            }
            return currentAvailableInvoker.invoke(invocation);
        }

        switch (step) {
            case APPLICATION_FIRST:
                currentAvailableInvoker = decideInvoker();
                break;
            case FORCE_APPLICATION:
                currentAvailableInvoker = serviceDiscoveryInvoker;
                break;
            case FORCE_INTERFACE:
            default:
                currentAvailableInvoker = invoker;
>>>>>>> origin/3.2
        }

        return currentAvailableInvoker.invoke(invocation);
    }

<<<<<<< HEAD
    @Override
    public boolean isAvailable() {
        return (invoker != null && invoker.isAvailable())
                || (serviceDiscoveryInvoker != null && serviceDiscoveryInvoker.isAvailable());
    }

    @Override
    public void destroy() {
=======
    private ClusterInvoker<T> decideInvoker() {
        if (currentAvailableInvoker == serviceDiscoveryInvoker) {
            if (checkInvokerAvailable(serviceDiscoveryInvoker)) {
                return serviceDiscoveryInvoker;
            }
            return invoker;
        } else {
            return currentAvailableInvoker;
        }
    }

    @Override
    public boolean isAvailable() {
        return currentAvailableInvoker != null
            ? currentAvailableInvoker.isAvailable()
            : (invoker != null && invoker.isAvailable()) || (serviceDiscoveryInvoker != null && serviceDiscoveryInvoker.isAvailable());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void destroy() {
        if (migrationRuleListener != null) {
            migrationRuleListener.removeMigrationInvoker(this);
        }
>>>>>>> origin/3.2
        if (invoker != null) {
            invoker.destroy();
        }
        if (serviceDiscoveryInvoker != null) {
            serviceDiscoveryInvoker.destroy();
        }
<<<<<<< HEAD
=======
        if (consumerModel != null) {
            Object object = consumerModel.getServiceMetadata().getAttribute(CommonConstants.CURRENT_CLUSTER_INVOKER_KEY);
            Map<Registry, MigrationInvoker<?>> invokerMap;
            if (object instanceof Map) {
                invokerMap = (Map<Registry, MigrationInvoker<?>>) object;
                invokerMap.remove(registry);
                if (invokerMap.isEmpty()) {
                    consumerModel.getServiceMetadata().getAttributeMap().remove(CommonConstants.CURRENT_CLUSTER_INVOKER_KEY);
                }
            }
        }
>>>>>>> origin/3.2
    }

    @Override
    public URL getUrl() {
<<<<<<< HEAD
        if (invoker != null) {
=======
        if (currentAvailableInvoker != null) {
            return currentAvailableInvoker.getUrl();
        } else if (invoker != null) {
>>>>>>> origin/3.2
            return invoker.getUrl();
        } else if (serviceDiscoveryInvoker != null) {
            return serviceDiscoveryInvoker.getUrl();
        }

        return consumerUrl;
    }

    @Override
    public URL getRegistryUrl() {
<<<<<<< HEAD
        if (invoker != null) {
            return invoker.getRegistryUrl();
        } else if (serviceDiscoveryInvoker != null) {
            serviceDiscoveryInvoker.getRegistryUrl();
=======
        if (currentAvailableInvoker != null) {
            return currentAvailableInvoker.getRegistryUrl();
        } else if (invoker != null) {
            return invoker.getRegistryUrl();
        } else if (serviceDiscoveryInvoker != null) {
            return serviceDiscoveryInvoker.getRegistryUrl();
>>>>>>> origin/3.2
        }
        return url;
    }

    @Override
    public Directory<T> getDirectory() {
<<<<<<< HEAD
        if (invoker != null) {
=======
        if (currentAvailableInvoker != null) {
            return currentAvailableInvoker.getDirectory();
        } else if (invoker != null) {
>>>>>>> origin/3.2
            return invoker.getDirectory();
        } else if (serviceDiscoveryInvoker != null) {
            return serviceDiscoveryInvoker.getDirectory();
        }
        return null;
    }

    @Override
    public boolean isDestroyed() {
<<<<<<< HEAD
        return (invoker == null || invoker.isDestroyed())
                && (serviceDiscoveryInvoker == null || serviceDiscoveryInvoker.isDestroyed());
    }


    @Override
    public AtomicBoolean invokersChanged() {
        return invokersChanged;
    }

    private volatile AtomicBoolean invokersChanged = new AtomicBoolean(true);

    private synchronized void compareAddresses(ClusterInvoker<T> serviceDiscoveryInvoker, ClusterInvoker<T> invoker) {
        this.invokersChanged.set(true);
        if (logger.isDebugEnabled()) {
            logger.debug(invoker.getDirectory().getAllInvokers() == null ? "null" : invoker.getDirectory().getAllInvokers().size() + "");
        }

        Set<MigrationAddressComparator> detectors = ExtensionLoader.getExtensionLoader(MigrationAddressComparator.class).getSupportedExtensionInstances();
        if (detectors != null && detectors.stream().allMatch(migrationDetector -> migrationDetector.shouldMigrate(serviceDiscoveryInvoker, invoker))) {
            discardInterfaceInvokerAddress(invoker);
        } else {
            discardServiceDiscoveryInvokerAddress(serviceDiscoveryInvoker);
        }
    }

    private synchronized void setAddressChanged() {
        this.invokersChanged.set(true);
    }

    @Override
    public synchronized void destroyServiceDiscoveryInvoker(ClusterInvoker<?> serviceDiscoveryInvoker) {
        if (checkInvokerAvailable(this.invoker)) {
            this.currentAvailableInvoker = this.invoker;
        }
        if (serviceDiscoveryInvoker != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Destroying instance address invokers, will not listen for address changes until re-subscribed, " + type.getName());
            }
            serviceDiscoveryInvoker.destroy();
        }
    }

    @Override
    public synchronized void discardServiceDiscoveryInvokerAddress(ClusterInvoker<?> serviceDiscoveryInvoker) {
        if (checkInvokerAvailable(this.invoker)) {
            this.currentAvailableInvoker = this.invoker;
        }
        if (serviceDiscoveryInvoker != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Discarding instance addresses, total size " + (null == serviceDiscoveryInvoker.getDirectory().getAllInvokers() ? "null" : serviceDiscoveryInvoker.getDirectory().getAllInvokers().size()));
            }
            serviceDiscoveryInvoker.getDirectory().discordAddresses();
        }
    }

    @Override
    public synchronized void refreshServiceDiscoveryInvoker() {
=======
        return currentAvailableInvoker != null
            ? currentAvailableInvoker.isDestroyed()
            : (invoker == null || invoker.isDestroyed()) && (serviceDiscoveryInvoker == null || serviceDiscoveryInvoker.isDestroyed());
    }

    @Override
    public boolean isServiceDiscovery() {
        return false;
    }

    @Override
    public MigrationStep getMigrationStep() {
        return step;
    }

    @Override
    public void setMigrationStep(MigrationStep step) {
        this.step = step;
    }

    @Override
    public MigrationRule getMigrationRule() {
        return rule;
    }

    @Override
    public void setMigrationRule(MigrationRule rule) {
        this.rule = rule;
        promotion = rule.getProportion(consumerUrl);
    }

    protected void destroyServiceDiscoveryInvoker() {
        if (this.invoker != null) {
            this.currentAvailableInvoker = this.invoker;
        }
        if (serviceDiscoveryInvoker != null && !serviceDiscoveryInvoker.isDestroyed()) {
            if (logger.isInfoEnabled()) {
                logger.info("Destroying instance address invokers, will not listen for address changes until re-subscribed, " + type.getName());
            }
            serviceDiscoveryInvoker.destroy();
            serviceDiscoveryInvoker = null;
        }
    }

    protected void refreshServiceDiscoveryInvoker(CountDownLatch latch) {
>>>>>>> origin/3.2
        clearListener(serviceDiscoveryInvoker);
        if (needRefresh(serviceDiscoveryInvoker)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Re-subscribing instance addresses, current interface " + type.getName());
            }
<<<<<<< HEAD
            serviceDiscoveryInvoker = registryProtocol.getServiceDiscoveryInvoker(cluster, registry, type, url);

            if (migrationMultiRegistry) {
                setListener(serviceDiscoveryInvoker, () -> {
                    this.setAddressChanged();
                });
            }
        }
    }

    private void clearListener(ClusterInvoker<T> invoker) {
        if (migrationMultiRegistry) {
            return;
        }

        if (invoker == null) {
            return;
        }
        DynamicDirectory<T> directory = (DynamicDirectory<T>) invoker.getDirectory();
        directory.setInvokersChangedListener(null);
    }

    private void setListener(ClusterInvoker<T> invoker, InvokersChangedListener listener) {
        if (invoker == null) {
            return;
        }
        DynamicDirectory<T> directory = (DynamicDirectory<T>) invoker.getDirectory();
        directory.setInvokersChangedListener(listener);
    }

    @Override
    public synchronized void refreshInterfaceInvoker() {
        clearListener(invoker);
        if (needRefresh(invoker)) {
            // FIXME invoker.destroy();
            if (logger.isDebugEnabled()) {
                logger.debug("Re-subscribing interface addresses for interface " + type.getName());
            }
            invoker = registryProtocol.getInvoker(cluster, registry, type, url);

            if (migrationMultiRegistry) {
                setListener(serviceDiscoveryInvoker, () -> {
                    this.setAddressChanged();
                });
            }
        }
    }

    @Override
    public synchronized void destroyInterfaceInvoker(ClusterInvoker<T> invoker) {
        if (checkInvokerAvailable(this.serviceDiscoveryInvoker)) {
            this.currentAvailableInvoker = this.serviceDiscoveryInvoker;
        }
        if (invoker != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Destroying interface address invokers, will not listen for address changes until re-subscribed, " + type.getName());
            }
            invoker.destroy();
        }
    }

    @Override
    public synchronized void discardInterfaceInvokerAddress(ClusterInvoker<T> invoker) {
        if (this.serviceDiscoveryInvoker != null) {
            this.currentAvailableInvoker = this.serviceDiscoveryInvoker;
        }
        if (invoker != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Discarding interface addresses, total address size " + (null == invoker.getDirectory().getAllInvokers() ? "null" : invoker.getDirectory().getAllInvokers().size()));
            }
            invoker.getDirectory().discordAddresses();
        }
    }

    private boolean needRefresh(ClusterInvoker<T> invoker) {
        return invoker == null || invoker.isDestroyed();
=======

            if (serviceDiscoveryInvoker != null) {
                serviceDiscoveryInvoker.destroy();
            }
            serviceDiscoveryInvoker = registryProtocol.getServiceDiscoveryInvoker(cluster, registry, type, url);
        }
        setListener(serviceDiscoveryInvoker, () -> {
            latch.countDown();
            if (reportService.hasReporter()) {
                reportService.reportConsumptionStatus(
                    reportService.createConsumptionReport(consumerUrl.getServiceInterface(), consumerUrl.getVersion(), consumerUrl.getGroup(), "app"));
            }
            if (step == APPLICATION_FIRST) {
                calcPreferredInvoker(rule);
            }
        });
    }

    protected void refreshInterfaceInvoker(CountDownLatch latch) {
        clearListener(invoker);
        if (needRefresh(invoker)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Re-subscribing interface addresses for interface " + type.getName());
            }

            if (invoker != null) {
                invoker.destroy();
            }
            invoker = registryProtocol.getInvoker(cluster, registry, type, url);
        }
        setListener(invoker, () -> {
            latch.countDown();
            if (reportService.hasReporter()) {
                reportService.reportConsumptionStatus(
                    reportService.createConsumptionReport(consumerUrl.getServiceInterface(), consumerUrl.getVersion(), consumerUrl.getGroup(), "interface"));
            }
            if (step == APPLICATION_FIRST) {
                calcPreferredInvoker(rule);
            }
        });
    }

    private synchronized void calcPreferredInvoker(MigrationRule migrationRule) {
        if (serviceDiscoveryInvoker == null || invoker == null) {
            return;
        }
        Set<MigrationAddressComparator> detectors = ScopeModelUtil.getApplicationModel(consumerUrl == null ? null : consumerUrl.getScopeModel())
            .getExtensionLoader(MigrationAddressComparator.class).getSupportedExtensionInstances();
        if (CollectionUtils.isNotEmpty(detectors)) {
            // pick preferred invoker
            // the real invoker choice in invocation will be affected by promotion
            if (detectors.stream().allMatch(comparator -> comparator.shouldMigrate(serviceDiscoveryInvoker, invoker, migrationRule))) {
                this.currentAvailableInvoker = serviceDiscoveryInvoker;
            } else {
                this.currentAvailableInvoker = invoker;
            }
        }
    }

    protected void destroyInterfaceInvoker() {
        if (this.serviceDiscoveryInvoker != null) {
            this.currentAvailableInvoker = this.serviceDiscoveryInvoker;
        }
        if (invoker != null && !invoker.isDestroyed()) {
            if (logger.isInfoEnabled()) {
                logger.info("Destroying interface address invokers, will not listen for address changes until re-subscribed, " + type.getName());
            }
            invoker.destroy();
            invoker = null;
        }
    }

    private void clearListener(ClusterInvoker<T> invoker) {
        if (invoker == null) {
            return;
        }
        DynamicDirectory<T> directory = (DynamicDirectory<T>) invoker.getDirectory();
        directory.setInvokersChangedListener(null);
    }

    private void setListener(ClusterInvoker<T> invoker, InvokersChangedListener listener) {
        if (invoker == null) {
            return;
        }
        DynamicDirectory<T> directory = (DynamicDirectory<T>) invoker.getDirectory();
        directory.setInvokersChangedListener(listener);
    }

    private boolean needRefresh(ClusterInvoker<T> invoker) {
        return invoker == null || invoker.isDestroyed() || !invoker.hasProxyInvokers();
>>>>>>> origin/3.2
    }

    public boolean checkInvokerAvailable(ClusterInvoker<T> invoker) {
        return invoker != null && !invoker.isDestroyed() && invoker.isAvailable();
    }

<<<<<<< HEAD
    @Override
    public boolean isServiceInvoker() {
        return false;
    }

    @Override
    public MigrationRule getMigrationRule() {
        return rule;
    }

    @Override
    public void setMigrationRule(MigrationRule rule) {
        this.rule = rule;
    }

    @Override
    public boolean isMigrationMultiRegistry() {
        return migrationMultiRegistry;
    }

=======
    protected void setCurrentAvailableInvoker(ClusterInvoker<T> currentAvailableInvoker) {
        this.currentAvailableInvoker = currentAvailableInvoker;
    }

    protected void setMigrationRuleListener(MigrationRuleListener migrationRuleListener) {
        this.migrationRuleListener = migrationRuleListener;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public URL getConsumerUrl() {
        return consumerUrl;
    }
>>>>>>> origin/3.2
}
