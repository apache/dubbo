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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.ClusterInvoker;

import java.util.List;

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
        float threshold;
        try {
            threshold = Float.parseFloat(rawThreshold);
        } catch (Exception e) {
            logger.error("Invalid migration threshold " + rawThreshold);
            threshold = DEFAULT_THREAD;
        }

        logger.info("Instance address size " + newAddressSize + ", interface address size " + oldAddressSize + ", threshold " + threshold);

        if (newAddressSize != 0 && oldAddressSize == 0) {
            return true;
        }
        if (newAddressSize == 0 && oldAddressSize == 0) {
            return false;
        }

        if (((float)newAddressSize / (float)oldAddressSize) >= threshold) {
            return true;
        }
        return false;
    }
}
