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
package com.alibaba.dubbo.rpc.cluster.interceptor;


import com.alibaba.dubbo.common.RegistryConstants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.ZoneDetector;
import com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker;

/**
 * Determines the zone information of current request.
 *
 * active only when url has key 'cluster=zone-aware'
 */
@Activate(value = "cluster:zone-aware")
public class ZoneAwareClusterInterceptor extends AbstractClusterInterceptor {

    @Override
    public void before(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {
        RpcContext rpcContext = RpcContext.getContext();
        String zone = (String) rpcContext.getAttachment(RegistryConstants.REGISTRY_ZONE);
        String force = (String) rpcContext.getAttachment(RegistryConstants.REGISTRY_ZONE_FORCE);
        ExtensionLoader<ZoneDetector> loader = ExtensionLoader.getExtensionLoader(ZoneDetector.class);
        if (StringUtils.isEmpty(zone) && loader.hasExtension("default")) {
            ZoneDetector detector = loader.getExtension("default");
            zone = detector.getZoneOfCurrentRequest(invocation);
            force = detector.isZoneForcingEnabled(invocation, zone);
        }

        if (StringUtils.isNotEmpty(zone)) {
            invocation.getAttachments().put(RegistryConstants.REGISTRY_ZONE, zone);
        }
        if (StringUtils.isNotEmpty(force)) {
            invocation.getAttachments().put(RegistryConstants.REGISTRY_ZONE_FORCE, force);
        }
    }

    @Override
    public void after(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {

    }
}
