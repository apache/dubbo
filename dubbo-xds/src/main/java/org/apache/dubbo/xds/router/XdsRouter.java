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
package org.apache.dubbo.xds.router;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.dubbo.xds.PilotExchanger;
import org.apache.dubbo.xds.resource.XdsCluster;
import org.apache.dubbo.xds.resource.XdsClusterWeight;
import org.apache.dubbo.xds.resource.XdsRoute;
import org.apache.dubbo.xds.resource.XdsVirtualHost;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class XdsRouter<T> extends AbstractStateRouter<T> {

    private final PilotExchanger pilotExchanger = PilotExchanger.getInstance();

    private Map<String, XdsVirtualHost> xdsVirtualHostMap = new ConcurrentHashMap<>();

    private Map<String, XdsCluster> xdsClusterMap = new ConcurrentHashMap<>();

    public XdsRouter(URL url) {
        super(url);
    }

    @Override
    protected BitList<Invoker<T>> doRoute(
            BitList<Invoker<T>> invokers,
            URL url,
            Invocation invocation,
            boolean needToPrintMessage,
            Holder<RouterSnapshotNode<T>> routerSnapshotNodeHolder,
            Holder<String> messageHolder)
            throws RpcException {

        // return all invokers directly if xds is not used
        // TODO：need to consider where to set ‘xds’ param
        if (!url.getParameter("xds", false)) {
            return invokers;
        }

        // 1. match cluster
        String matchedCluster = matchCluster(invocation);

        // 2. match invokers
        BitList<Invoker<T>> matchedInvokers = matchInvoker(matchedCluster, invokers);

        return matchedInvokers;
    }

    private String matchCluster(Invocation invocation) {
        String cluster = null;
        String serviceName = invocation.getInvoker().getUrl().getParameter("providedBy");
        XdsVirtualHost xdsVirtualHost = pilotExchanger.getXdsVirtualHostMap().get(serviceName);

        // match route
        for (XdsRoute xdsRoute : xdsVirtualHost.getRoutes()) {
            // match path
            String path = "/" + invocation.getInvoker().getUrl().getPath() + "/" + RpcUtils.getMethodName(invocation);
            if (xdsRoute.getRouteMatch().isMatch(path)) {
                cluster = xdsRoute.getRouteAction().getCluster();

                // if weighted cluster
                if (cluster == null) {
                    cluster = computeWeightCluster(xdsRoute.getRouteAction().getClusterWeights());
                }
            }
        }

        return cluster;
    }

    private String computeWeightCluster(List<XdsClusterWeight> weightedClusters) {
        int totalWeight = Math.max(
                weightedClusters.stream().mapToInt(XdsClusterWeight::getWeight).sum(), 1);

        int target = ThreadLocalRandom.current().nextInt(1, totalWeight + 1);
        for (XdsClusterWeight xdsClusterWeight : weightedClusters) {
            int weight = xdsClusterWeight.getWeight();
            target -= weight;
            if (target <= 0) {
                return xdsClusterWeight.getName();
            }
        }
        return null;
    }

    private BitList<Invoker<T>> matchInvoker(String clusterName, BitList<Invoker<T>> invokers) {

        List<Invoker<T>> filterInvokers = invokers.stream()
                .filter(inv -> inv.getUrl().getParameter("clusterID").equals(clusterName))
                .collect(Collectors.toList());
        return new BitList<>(filterInvokers);
    }
}
