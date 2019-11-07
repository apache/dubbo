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
package org.apache.dubbo.rpc.cluster.interceptor;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.ZoneDetector;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_ZONE;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_ZONE_FORCE;

/**
 * Determines the zone information of current request.
 *
 * active only when url has key 'cluster=zone-aware'
 */
@Activate(value = "cluster:zone-aware")
public class ZoneAwareClusterInterceptor implements ClusterInterceptor {

    @Override
    public void before(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {
        RpcContext rpcContext = RpcContext.getContext();
        String zone = (String) rpcContext.getAttachment(REGISTRY_ZONE);
        String force = (String) rpcContext.getAttachment(REGISTRY_ZONE_FORCE);
        ExtensionLoader<ZoneDetector> loader = ExtensionLoader.getExtensionLoader(ZoneDetector.class);
        if (StringUtils.isEmpty(zone) && loader.hasExtension("default")) {
            ZoneDetector detector = loader.getExtension("default");
            zone = detector.getZoneOfCurrentRequest(invocation);
            force = detector.isZoneForcingEnabled(invocation, zone);
        }

        if (StringUtils.isNotEmpty(zone)) {
            invocation.setAttachment(REGISTRY_ZONE, zone);
        }
        if (StringUtils.isNotEmpty(force)) {
            invocation.setAttachment(REGISTRY_ZONE_FORCE, force);
        }
    }

    @Override
    public void after(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {

    }
}
