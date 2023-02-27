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
<<<<<<< HEAD
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
=======
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.migration.model.MigrationRule;
>>>>>>> origin/3.2
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;

import java.util.List;
<<<<<<< HEAD

public class DefaultMigrationAddressComparator implements MigrationAddressComparator {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMigrationAddressComparator.class);
    private static final String MIGRATION_THRESHOLD = "dubbo.application.migration.threshold";
    private static final String DEFAULT_THRESHOLD_STRING = "0.8";
    private static final float DEFAULT_THREAD = 0.8f;

    @Override
    public <T> boolean shouldMigrate(ClusterInvoker<T> serviceDiscoveryInvoker, ClusterInvoker<T> invoker) {
        if (!serviceDiscoveryInvoker.isAvailable()) {
            logger.info("No instance address available, will not migrate.");
            return false;
        }
        if (!invoker.isAvailable()) {
            logger.info("No interface address available, will migrate.");
            return true;
        }

        List<Invoker<T>> invokers1 = serviceDiscoveryInvoker.getDirectory().getAllInvokers();
        List<Invoker<T>> invokers2 = invoker.getDirectory().getAllInvokers();

        int newAddressSize = CollectionUtils.isNotEmpty(invokers1) ? invokers1.size() : 0;
        int oldAddressSize = CollectionUtils.isNotEmpty(invokers2) ? invokers2.size() : 0;

        String rawThreshold = ConfigurationUtils.getDynamicProperty(MIGRATION_THRESHOLD, DEFAULT_THRESHOLD_STRING);
=======
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.COMMON_PROPERTY_TYPE_MISMATCH;

public class DefaultMigrationAddressComparator implements MigrationAddressComparator {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(DefaultMigrationAddressComparator.class);
    private static final String MIGRATION_THRESHOLD = "dubbo.application.migration.threshold";
    private static final String DEFAULT_THRESHOLD_STRING = "0.0";
    private static final float DEFAULT_THREAD = 0f;

    public static final String OLD_ADDRESS_SIZE = "OLD_ADDRESS_SIZE";
    public static final String NEW_ADDRESS_SIZE = "NEW_ADDRESS_SIZE";

    private final ConcurrentMap<String, Map<String, Integer>> serviceMigrationData = new ConcurrentHashMap<>();

    @Override
    public <T> boolean shouldMigrate(ClusterInvoker<T> newInvoker, ClusterInvoker<T> oldInvoker, MigrationRule rule) {
        Map<String, Integer> migrationData = ConcurrentHashMapUtils.computeIfAbsent(serviceMigrationData, oldInvoker.getUrl().getDisplayServiceKey(), _k -> new ConcurrentHashMap<>());

        if (!newInvoker.hasProxyInvokers()) {
            migrationData.put(OLD_ADDRESS_SIZE, getAddressSize(oldInvoker));
            migrationData.put(NEW_ADDRESS_SIZE, -1);
            logger.info("No " + getInvokerType(newInvoker) + " address available, stop compare.");
            return false;
        }
        if (!oldInvoker.hasProxyInvokers()) {
            migrationData.put(OLD_ADDRESS_SIZE, -1);
            migrationData.put(NEW_ADDRESS_SIZE, getAddressSize(newInvoker));
            logger.info("No " + getInvokerType(oldInvoker) + " address available, stop compare.");
            return true;
        }

        int newAddressSize = getAddressSize(newInvoker);
        int oldAddressSize = getAddressSize(oldInvoker);

        migrationData.put(OLD_ADDRESS_SIZE, oldAddressSize);
        migrationData.put(NEW_ADDRESS_SIZE, newAddressSize);

        String rawThreshold = null;
        Float configuredThreshold = rule == null ? null : rule.getThreshold(oldInvoker.getUrl());
        if (configuredThreshold != null && configuredThreshold >= 0) {
            rawThreshold = String.valueOf(configuredThreshold);
        }
        rawThreshold = StringUtils.isNotEmpty(rawThreshold) ? rawThreshold : ConfigurationUtils.getCachedDynamicProperty(newInvoker.getUrl().getScopeModel(), MIGRATION_THRESHOLD, DEFAULT_THRESHOLD_STRING);
>>>>>>> origin/3.2
        float threshold;
        try {
            threshold = Float.parseFloat(rawThreshold);
        } catch (Exception e) {
<<<<<<< HEAD
            logger.error("Invalid migration threshold " + rawThreshold);
            threshold = DEFAULT_THREAD;
        }

        logger.info("Instance address size " + newAddressSize + ", interface address size " + oldAddressSize + ", threshold " + threshold);
=======
            logger.error(COMMON_PROPERTY_TYPE_MISMATCH, "", "", "Invalid migration threshold " + rawThreshold);
            threshold = DEFAULT_THREAD;
        }

        logger.info("serviceKey:" + oldInvoker.getUrl().getServiceKey() + " Instance address size " + newAddressSize + ", interface address size " + oldAddressSize + ", threshold " + threshold);
>>>>>>> origin/3.2

        if (newAddressSize != 0 && oldAddressSize == 0) {
            return true;
        }
        if (newAddressSize == 0 && oldAddressSize == 0) {
            return false;
        }

<<<<<<< HEAD
        if (((float)newAddressSize / (float)oldAddressSize) >= threshold) {
            return true;
        }
        return false;
=======
        return ((float) newAddressSize / (float) oldAddressSize) >= threshold;
    }

    private <T> int getAddressSize(ClusterInvoker<T> invoker) {
        if (invoker == null) {
            return -1;
        }
        List<Invoker<T>> invokers = invoker.getDirectory().getAllInvokers();
        return CollectionUtils.isNotEmpty(invokers) ? invokers.size() : 0;
    }

    @Override
    public Map<String, Integer> getAddressSize(String displayServiceKey) {
        return serviceMigrationData.get(displayServiceKey);
    }

    private String getInvokerType(ClusterInvoker<?> invoker) {
        if (invoker.isServiceDiscovery()) {
            return "instance";
        }
        return "interface";
>>>>>>> origin/3.2
    }
}
