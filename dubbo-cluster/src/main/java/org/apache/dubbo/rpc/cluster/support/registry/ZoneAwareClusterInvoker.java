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
package org.apache.dubbo.rpc.cluster.support.registry;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.ZoneDetector;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
<<<<<<< HEAD
import org.apache.dubbo.rpc.cluster.support.migration.MigrationClusterComparator;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationClusterInvoker;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationRule;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationStep;
import org.apache.dubbo.rpc.cluster.support.wrapper.MockClusterInvoker;
=======
>>>>>>> origin/3.2

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

<<<<<<< HEAD
import static org.apache.dubbo.common.constants.RegistryConstants.LOADBALANCE_AMONG_REGISTRIES;
=======
import static org.apache.dubbo.common.constants.CommonConstants.PREFERRED_KEY;
>>>>>>> origin/3.2
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_ZONE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_ZONE_FORCE;
import static org.apache.dubbo.common.constants.RegistryConstants.ZONE_KEY;
import static org.apache.dubbo.config.RegistryConfig.PREFER_REGISTRY_KEY;

/**
 * When there are more than one registry for subscription.
 * <p>
 * This extension provides a strategy to decide how to distribute traffics among them:
 * 1. registry marked as 'preferred=true' has the highest priority.
 * 2. check the zone the current request belongs, pick the registry that has the same zone first.
 * 3. Evenly balance traffic between all registries based on each registry's weight.
 */
public class ZoneAwareClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(ZoneAwareClusterInvoker.class);

<<<<<<< HEAD
    private final LoadBalance loadBalanceAmongRegistries = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(LOADBALANCE_AMONG_REGISTRIES);
=======
    private ZoneDetector zoneDetector;
>>>>>>> origin/3.2

    public ZoneAwareClusterInvoker(Directory<T> directory) {
        super(directory);
        ExtensionLoader<ZoneDetector> loader = directory.getConsumerUrl().getOrDefaultApplicationModel().getExtensionLoader(ZoneDetector.class);
        if (loader.hasExtension("default")) {
            zoneDetector = loader.getExtension("default");
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        // First, pick the invoker (XXXClusterInvoker) that comes from the local registry, distinguish by a 'preferred' key.
        for (Invoker<T> invoker : invokers) {
            ClusterInvoker<T> clusterInvoker = (ClusterInvoker<T>) invoker;
            if (clusterInvoker.isAvailable() && clusterInvoker.getRegistryUrl()
<<<<<<< HEAD
                    .getParameter(PREFER_REGISTRY_KEY, false)) {
=======
                    .getParameter(PREFERRED_KEY, false)) {
>>>>>>> origin/3.2
                return clusterInvoker.invoke(invocation);
            }
        }

        RpcContext rpcContext = RpcContext.getClientAttachment();
        String zone = rpcContext.getAttachment(REGISTRY_ZONE);
        String force = rpcContext.getAttachment(REGISTRY_ZONE_FORCE);
        if (StringUtils.isEmpty(zone) && zoneDetector != null) {
            zone = zoneDetector.getZoneOfCurrentRequest(invocation);
            force = zoneDetector.isZoneForcingEnabled(invocation, zone);
        }

        // providers in the registry with the same zone
        if (StringUtils.isNotEmpty(zone)) {
            for (Invoker<T> invoker : invokers) {
                ClusterInvoker<T> clusterInvoker = (ClusterInvoker<T>) invoker;
                if (clusterInvoker.isAvailable() && zone.equals(clusterInvoker.getRegistryUrl().getParameter(ZONE_KEY))) {
                    return clusterInvoker.invoke(invocation);
                }
            }
            if (StringUtils.isNotEmpty(force) && "true".equalsIgnoreCase(force)) {
                throw new IllegalStateException("No registry instance in zone or no available providers in the registry, zone: "
                        + zone
                        + ", registries: " + invokers.stream().map(invoker -> ((ClusterInvoker<T>) invoker).getRegistryUrl().toString()).collect(Collectors.joining(",")));
            }
        }


        // load balance among all registries, with registry weight count in.
<<<<<<< HEAD
        Invoker<T> balancedInvoker = select(loadBalanceAmongRegistries, invocation, invokers, null);
        if (balancedInvoker.isAvailable()) {
=======
        Invoker<T> balancedInvoker = select(loadbalance, invocation, invokers, null);
        if (balancedInvoker!=null && balancedInvoker.isAvailable()) {
>>>>>>> origin/3.2
            return balancedInvoker.invoke(invocation);
        }

        // If none of the invokers has a preferred signal or is picked by the loadbalancer, pick the first one available.
        for (Invoker<T> invoker : invokers) {
            ClusterInvoker<T> clusterInvoker = (ClusterInvoker<T>) invoker;
            if (clusterInvoker.isAvailable()) {
                return clusterInvoker.invoke(invocation);
            }
        }

        throw new RpcException("No provider available in " + invokers);
    }

<<<<<<< HEAD
    @Override
    protected List<Invoker<T>> list(Invocation invocation) throws RpcException {
        List<Invoker<T>> invokers = super.list(invocation);

        if (null == invokers || invokers.size() < 2) {
            return invokers;
        }

        List<Invoker<T>> interfaceInvokers = new ArrayList<>();
        List<Invoker<T>> serviceInvokers = new ArrayList<>();
        boolean addressChanged = false;
        for (Invoker<T> invoker : invokers) {
            MigrationClusterInvoker migrationClusterInvoker = (MigrationClusterInvoker) invoker;
            if (migrationClusterInvoker.isServiceInvoker()) {
                serviceInvokers.add(invoker);
            } else {
                interfaceInvokers.add(invoker);
            }

            if (migrationClusterInvoker.invokersChanged().compareAndSet(true, false)) {
                addressChanged = true;
            }
        }

        if (serviceInvokers.isEmpty() || interfaceInvokers.isEmpty()) {
            return invokers;
        }

        MigrationRule rule = null;
        for (Invoker<T> invoker : serviceInvokers) {
            MigrationClusterInvoker migrationClusterInvoker = (MigrationClusterInvoker) invoker;

            if (rule == null) {
                rule = migrationClusterInvoker.getMigrationRule();
                continue;
            }

            // inconsistency rule
            if (!rule.equals(migrationClusterInvoker.getMigrationRule())) {
                rule = MigrationRule.queryRule();
                break;
            }
        }

        MigrationStep step = rule.getStep();

        switch (step) {
            case FORCE_INTERFACE:
                clusterRefresh(addressChanged, interfaceInvokers);
                clusterDestroy(addressChanged, serviceInvokers, true);
                if (logger.isDebugEnabled()) {
                    logger.debug("step is FORCE_INTERFACE");
                }
                return interfaceInvokers;

            case APPLICATION_FIRST:
                clusterRefresh(addressChanged, serviceInvokers);
                clusterRefresh(addressChanged, interfaceInvokers);

                boolean serviceAvailable = !serviceInvokers.isEmpty();
                if (serviceAvailable) {
                    if (shouldMigrate(addressChanged, serviceInvokers, interfaceInvokers)) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("step is APPLICATION_FIRST shouldMigrate true get serviceInvokers");
                        }
                        return serviceInvokers;
                    }
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("step is APPLICATION_FIRST " + (serviceInvokers.isEmpty() ? "serviceInvokers is empty" : "shouldMigrate false") + " get interfaceInvokers");
                }

                return interfaceInvokers;

            case FORCE_APPLICATION:
                clusterRefresh(addressChanged, serviceInvokers);
                clusterDestroy(addressChanged, interfaceInvokers, true);

                if (logger.isDebugEnabled()) {
                    logger.debug("step is FORCE_APPLICATION");
                }

                return serviceInvokers;
        }

        throw new UnsupportedOperationException(rule.getStep().name());
    }


    private boolean shouldMigrate(boolean addressChanged, List<Invoker<T>> serviceInvokers, List<Invoker<T>> interfaceInvokers) {
        Set<MigrationClusterComparator> detectors = ExtensionLoader.getExtensionLoader(MigrationClusterComparator.class).getSupportedExtensionInstances();
        if (detectors != null && !detectors.isEmpty()) {
            return detectors.stream().allMatch(s -> s.shouldMigrate(interfaceInvokers, serviceInvokers));
        }

        // check application level provider available.
        List<Invoker<T>> availableServiceInvokers = serviceInvokers.stream().filter(s -> s.isAvailable()).collect(Collectors.toList());
        return !availableServiceInvokers.isEmpty();
    }

    private void clusterDestroy(boolean addressChanged, List<Invoker<T>> invokers, boolean destroySub) {
        if (addressChanged) {
            invokers.forEach(s -> {
                MigrationClusterInvoker invoker = (MigrationClusterInvoker) s;
                if (invoker.isServiceInvoker()) {
                    invoker.discardServiceDiscoveryInvokerAddress(invoker);
                    if (destroySub) {
                        invoker.destroyServiceDiscoveryInvoker(invoker);
                    }
                } else {
                    invoker.discardInterfaceInvokerAddress(invoker);
                    if (destroySub) {
                        invoker.destroyInterfaceInvoker(invoker);
                    }
                }
            });
        }
    }

    private void clusterRefresh(boolean addressChanged, List<Invoker<T>> invokers) {
        if (addressChanged) {
            invokers.forEach(s -> {
                MigrationClusterInvoker invoker = (MigrationClusterInvoker) s;
                if (invoker.isServiceInvoker()) {
                    invoker.refreshServiceDiscoveryInvoker();
                } else {
                    invoker.refreshInterfaceInvoker();
                }
            });
        }
    }

=======
>>>>>>> origin/3.2
}
