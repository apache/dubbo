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
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationCluserInvoker;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationClusterComparator;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationRule;
import org.apache.dubbo.rpc.cluster.support.migration.MigrationStep;
import org.apache.dubbo.rpc.cluster.support.wrapper.MockClusterInvoker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.PREFERRED_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.*;

/**
 * When there're more than one registry for subscription.
 * <p>
 * This extension provides a strategy to decide how to distribute traffics among them:
 * 1. registry marked as 'preferred=true' has the highest priority.
 * 2. check the zone the current request belongs, pick the registry that has the same zone first.
 * 3. Evenly balance traffic between all registries based on each registry's weight.
 * 4. Pick anyone that's available.
 */
public class ZoneAwareClusterInvoker<T> extends AbstractClusterInvoker<T> {

    private static final Logger logger = LoggerFactory.getLogger(ZoneAwareClusterInvoker.class);

    public ZoneAwareClusterInvoker(Directory<T> directory) {
        super(directory);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result doInvoke(Invocation invocation, final List<Invoker<T>> invokers, LoadBalance loadbalance) throws RpcException {
        // First, pick the invoker (XXXClusterInvoker) that comes from the local registry, distinguish by a 'preferred' key.
        for (Invoker<T> invoker : invokers) {
            ClusterInvoker<T> clusterInvoker = (ClusterInvoker<T>) invoker;
            if (clusterInvoker.isAvailable() && clusterInvoker.getRegistryUrl()
                    .getParameter(REGISTRY_KEY + "." + PREFERRED_KEY, false)) {
                return clusterInvoker.invoke(invocation);
            }
        }

        // providers in the registry with the same zone
        String zone = invocation.getAttachment(REGISTRY_ZONE);
        if (StringUtils.isNotEmpty(zone)) {
            for (Invoker<T> invoker : invokers) {
                ClusterInvoker<T> clusterInvoker = (ClusterInvoker<T>) invoker;
                if (clusterInvoker.isAvailable() && zone.equals(clusterInvoker.getRegistryUrl().getParameter(REGISTRY_KEY + "." + ZONE_KEY))) {
                    return clusterInvoker.invoke(invocation);
                }
            }
            String force = invocation.getAttachment(REGISTRY_ZONE_FORCE);
            if (StringUtils.isNotEmpty(force) && "true".equalsIgnoreCase(force)) {
                throw new IllegalStateException("No registry instance in zone or no available providers in the registry, zone: "
                        + zone
                        + ", registries: " + invokers.stream().map(invoker -> ((MockClusterInvoker<T>) invoker).getRegistryUrl().toString()).collect(Collectors.joining(",")));
            }
        }


        // load balance among all registries, with registry weight count in.
        Invoker<T> balancedInvoker = select(loadbalance, invocation, invokers, null);
        if (balancedInvoker.isAvailable()) {
            return balancedInvoker.invoke(invocation);
        }

        // If none of the invokers has a preferred signal or is picked by the loadbalancer, pick the first one available.
        for (Invoker<T> invoker : invokers) {
            ClusterInvoker<T> clusterInvoker = (ClusterInvoker<T>) invoker;
            if (clusterInvoker.isAvailable()) {
                return clusterInvoker.invoke(invocation);
            }
        }

        //if none available,just pick one
        return invokers.get(0).invoke(invocation);
    }

    @Override
    protected List<Invoker<T>> list(Invocation invocation) throws RpcException {
        List<Invoker<T>> invokers = super.list(invocation);

        if (null == invokers || invokers.size() < 2) {
            return invokers;
        }

        // 开关
        //

        //List<Invoker<T>>  interfaceInvokers = invokers.stream().filter( s -> !((MigrationCluserInvoker)s).isServiceInvoker()).collect(Collectors.toList());
        //List<Invoker<T>>  serviceInvokers = invokers.stream().filter( s -> ((MigrationCluserInvoker)s).isServiceInvoker()).collect(Collectors.toList());

        List<Invoker<T>>  interfaceInvokers = new ArrayList<>();
        List<Invoker<T>>  serviceInvokers = new ArrayList<>();
        boolean addreddChanged = false;
        for (Invoker<T> invoker : invokers) {
            MigrationCluserInvoker migrationCluserInvoker = (MigrationCluserInvoker) invoker;
            if (migrationCluserInvoker.isServiceInvoker()) {
                serviceInvokers.add(invoker);
            } else {
                interfaceInvokers.add(invoker);
            }

            if (migrationCluserInvoker.addressChanged().compareAndSet(false, true)) {
                addreddChanged = true;
            }
        }

        if (serviceInvokers.isEmpty() || interfaceInvokers.isEmpty()) {
            return invokers;
        }

        MigrationRule rule = null;

        for (Invoker<T> invoker : serviceInvokers) {
            MigrationCluserInvoker migrationCluserInvoker = (MigrationCluserInvoker) invoker;
            if (null == rule) {
                rule = migrationCluserInvoker.getMigrationRule();
            } else {
                // 不一致
                if (!rule.equals(migrationCluserInvoker.getMigrationRule())) {
                    rule = MigrationRule.queryRule();
                    break;
                }
            }
        }

        MigrationStep step = rule.getStep();

        // 地址比对
        if (MigrationStep.APPLICATION_FIRST == step && addreddChanged) {
            Set<MigrationClusterComparator> detectors = ExtensionLoader.getExtensionLoader(MigrationClusterComparator.class).getSupportedExtensionInstances();
            if (null != detectors) {
                if (!detectors.stream().anyMatch(s -> s.compare(interfaceInvokers, serviceInvokers))) {
                    step = MigrationStep.INTERFACE_FIRST;
                }
            }
        }

        switch (step) {
            case INTERFACE_FIRST:
                if (interfaceInvokers.stream().filter(s -> s.isAvailable()).collect(Collectors.toList()).isEmpty()) {
                    clusterRefresh(addreddChanged, interfaceInvokers);
                }

                if (interfaceInvokers.stream().filter(s -> s.isAvailable()).collect(Collectors.toList()).isEmpty()) {
                    return serviceInvokers;
                } else {
                    clusterDestory(addreddChanged, serviceInvokers);
                    clusterRefresh(addreddChanged, interfaceInvokers);
                    return interfaceInvokers;
                }
            case APPLICATION_FIRST:

                if (serviceInvokers.isEmpty()) {
                    clusterRefresh(addreddChanged, interfaceInvokers);
                    return  interfaceInvokers;
                }

                List<Invoker<T>>  availableServiceInvokers = serviceInvokers.stream().filter( s -> ((MigrationCluserInvoker)s).isAvailable()).collect(Collectors.toList());
                if (availableServiceInvokers.isEmpty()) {
                    clusterRefresh(addreddChanged, serviceInvokers);
                    availableServiceInvokers = serviceInvokers.stream().filter( s -> ((MigrationCluserInvoker)s).isAvailable()).collect(Collectors.toList());
                }

                if (availableServiceInvokers.size() > 0) {
                    clusterDestory(addreddChanged, interfaceInvokers);
                    return availableServiceInvokers;
                } else {
                    clusterRefresh(addreddChanged, interfaceInvokers);
                    return interfaceInvokers.stream().filter( s -> ((MigrationCluserInvoker)s).isAvailable()).collect(Collectors.toList());
                }


            case FORCE_APPLICATION:
                clusterRefresh(addreddChanged, serviceInvokers);
                clusterDestory(addreddChanged, interfaceInvokers);

                return serviceInvokers;
        }

        throw new UnsupportedOperationException(rule.getStep().name());
    }


    private void clusterDestory(boolean addressChanged, List<Invoker<T>> invokers) {
        if (addressChanged) {
            invokers.forEach(s -> {
                MigrationCluserInvoker invoker = (MigrationCluserInvoker)s;
                if (invoker.isServiceInvoker()) {
                    invoker.discardServiceDiscoveryInvokerAddress();
                    invoker.destroyServiceDiscoveryInvoker();
                } else {
                    invoker.discardInterfaceInvokerAddress();
                    invoker.destroyInterfaceInvoker();
                }
            });
        }
    }

    private void clusterRefresh(boolean addressChanged, List<Invoker<T>> invokers) {
        if (addressChanged) {
            invokers.forEach( s -> {
                MigrationCluserInvoker invoker = (MigrationCluserInvoker)s;
                if (invoker.isServiceInvoker()) {
                    invoker.refreshServiceDiscoveryInvoker();
                } else {
                    invoker.refreshInterfaceInvoker();
                }
            });
        }
    }

}