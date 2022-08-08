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
package org.apache.dubbo.registry.xds.util;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.xds.istio.IstioEnv;

import io.envoyproxy.envoy.config.core.v3.Node;

public class NodeBuilder {

    private final static String SVC_CLUSTER_LOCAL = ".svc.cluster.local";

    public static Node build() {
//        String podName = System.getenv("metadata.name");
//        String podNamespace = System.getenv("metadata.namespace");

        String podName = IstioEnv.getInstance().getPodName();
        String podNamespace = IstioEnv.getInstance().getWorkloadNameSpace();
        String svcName = IstioEnv.getInstance().getIstioMetaClusterId();

        // id -> sidecar~ip~{POD_NAME}~{NAMESPACE_NAME}.svc.cluster.local
        // cluster -> {SVC_NAME}
        return Node.newBuilder()
            .setId("sidecar~" + NetUtils.getLocalHost() + "~" +podName + "~" + podNamespace + SVC_CLUSTER_LOCAL)
            .setCluster(svcName)
            .build();
    }
}
