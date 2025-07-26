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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.apache.dubbo.xds.XdsResourceFactory;
import org.apache.dubbo.xds.resource.route.ClusterWeight;
import org.apache.dubbo.xds.resource.route.Route;
import org.apache.dubbo.xds.resource.route.VirtualHost;
import org.apache.dubbo.xds.resource.update.CdsUpdate;
import org.apache.dubbo.xds.resource.update.CdsUpdate.ClusterType;
import org.apache.dubbo.xds.resource.update.EdsUpdate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.apache.dubbo.config.Constants.MESH_KEY;

public class XdsRouter<T> extends AbstractStateRouter<T> {

    private final static ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsRouter.class);
    private static final String XDS_ROUTER_CLUSTER_KEY = "xds.router.cluster";
    private final XdsResourceFactory xdsResourceFactory = XdsResourceFactory.getInstance();

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

        logger.info("[XDS-ROUTER] XdsRouter.doRoute called with {} invokers [Thread: {}]", invokers.size(), Thread.currentThread().getName());
        
        // 记录所有输入的invoker信息
        for (int i = 0; i < invokers.size(); i++) {
            Invoker<T> invoker = invokers.get(i);
            logger.info("[XDS-ROUTER] Input invoker[{}]: {} (clusterID: {}) [Thread: {}]",
                i, invoker.getUrl().getAddress(), invoker.getUrl().getParameter("clusterID"), Thread.currentThread().getName());
        }

        if (invokers.isEmpty()) {
            logger.info("[XDS-ROUTER] Invokers list is empty, returning empty list.");
            return invokers;
        }

        // 检查 invocation 中是否已经记录了选择的 cluster
        String selectedCluster = invocation.getAttachment(XDS_ROUTER_CLUSTER_KEY);
        if (StringUtils.isNotEmpty(selectedCluster)) {
            logger.info("[XDS-ROUTER] Using previously selected cluster from invocation attachment: {} [Thread: {}]",
                selectedCluster, Thread.currentThread().getName());
        } else {
            String serviceName = invocation.getInvoker().getUrl().getParameter("provided-by");
            if (StringUtils.isEmpty(serviceName)) {
                serviceName = getUrl().getServiceInterface();
            }
            VirtualHost virtualHost = xdsResourceFactory.getXdsVirtualHostMap().get(serviceName);

            selectedCluster = matchCluster(url, virtualHost, invocation);
            if (StringUtils.isNotEmpty(selectedCluster)) {
                logger.info("[XDS-ROUTER] Storing selected cluster in invocation attachment: {} [Thread: {}]",
                    selectedCluster, Thread.currentThread().getName());
                // 将选择的 cluster 存入 invocation，以便在同一次调用中保持一致
                invocation.setAttachment(XDS_ROUTER_CLUSTER_KEY, selectedCluster);
            }
        }

        if (StringUtils.isEmpty(selectedCluster)) {
            logger.warn("[XDS-ROUTER] No cluster matched, returning all invokers. [Thread: {}]", Thread.currentThread().getName());
            return invokers;
        }

        logger.info("[XDS-ROUTER] Matched cluster: {} [Thread: {}]", selectedCluster, Thread.currentThread().getName());
        
        BitList<Invoker<T>> result = matchInvoker(selectedCluster, invokers);

        logger.info("[XDS-ROUTER] Router selected {} invokers [Thread: {}]", result.size(), Thread.currentThread().getName());
        for (int i = 0; i < result.size(); i++) {
            Invoker<T> invoker = result.get(i);
            logger.info("[XDS-ROUTER] Selected invoker[{}]: {} (clusterID: {}) [Thread: {}]",
                i, invoker.getUrl().getAddress(), invoker.getUrl().getParameter("clusterID"), Thread.currentThread().getName());
        }
        
        // Final check before returning
        logger.info("[XDS-ROUTER] *** FINAL RETURN CHECK *** [Thread: {}]", Thread.currentThread().getName());
        logger.info("[XDS-ROUTER] About to return {} invokers [Thread: {}]", result.size(), Thread.currentThread().getName());
        for(int i = 0; i < result.size(); i++) {
            Invoker<T> invoker = result.get(i);
            logger.info("[XDS-ROUTER] RETURNING invoker[{}]: {} (clusterID: {}) [Thread: {}]", i, invoker.getUrl().getAddress(), invoker.getUrl().getParameter("clusterID"), Thread.currentThread().getName());
        }

        return result;
    }

    private String matchCluster(URL url, VirtualHost virtualHost, Invocation invocation) {
        String cluster = null;
        String serviceName = invocation.getInvoker().getUrl().getParameter("provided-by");
        
        // 添加详细的调试日志
        logger.info("[XDS] XdsRouter.matchCluster called for serviceName: {} [Thread: {}]", serviceName, Thread.currentThread().getName());
        logger.info("[XDS] Current xdsVirtualHostMap keys: {} [Thread: {}]", xdsResourceFactory.getXdsVirtualHostMap().keySet(), Thread.currentThread().getName());
        
        VirtualHost xdsVirtualHost = xdsResourceFactory.getXdsVirtualHostMap().get(serviceName);
        
        if (xdsVirtualHost == null) {
            logger.error("[XDS] Cannot find VirtualHost for serviceName: {} [Thread: {}]", serviceName, Thread.currentThread().getName());
            logger.error("[XDS] Available VirtualHost keys: {} [Thread: {}]", xdsResourceFactory.getXdsVirtualHostMap().keySet(), Thread.currentThread().getName());
            
            // 尝试使用不同的key格式查找
            String[] possibleKeys = {
                serviceName,
                serviceName.contains(":") ? serviceName.substring(0, serviceName.indexOf(":")) : serviceName,
                serviceName.contains(".") ? serviceName.substring(0, serviceName.indexOf(".")) : serviceName
            };
            
            for (String key : possibleKeys) {
                VirtualHost vh = xdsResourceFactory.getXdsVirtualHostMap().get(key);
                if (vh != null) {
                    logger.info("[XDS] Found VirtualHost with alternative key: {} [Thread: {}]", key, Thread.currentThread().getName());
                    xdsVirtualHost = vh;
                    break;
                }
            }
            
            if (xdsVirtualHost == null) {
                logger.error("[XDS] Still cannot find VirtualHost after trying alternative keys [Thread: {}]", Thread.currentThread().getName());
                return null;
            }
        } else {
            logger.info("[XDS] Found VirtualHost: {} for serviceName: {} [Thread: {}]", xdsVirtualHost.getName(), serviceName, Thread.currentThread().getName());
        }

        logger.info("[XDS] VirtualHost has {} routes [Thread: {}]", xdsVirtualHost.getRoutes().size(), Thread.currentThread().getName());

        // match route
        for (Route xdsRoute : xdsVirtualHost.getRoutes()) {
            // match path
            String path = "/" + invocation.getInvoker().getUrl().getPath() + "/" + RpcUtils.getMethodName(invocation);
            logger.info("[XDS] Trying to match path: {} against route: {} [Thread: {}]", path, xdsRoute.getRouteMatch(), Thread.currentThread().getName());
            
            if (xdsRoute.getRouteMatch().isPathMatch(path)) {
                logger.info("[XDS] Path matched! Route action: {} [Thread: {}]", xdsRoute.getRouteAction(), Thread.currentThread().getName());
                cluster = xdsRoute.getRouteAction().getCluster();
                // if weighted cluster
                if (cluster == null) {
                    logger.info("[XDS] No direct cluster, checking weighted clusters: {} [Thread: {}]", xdsRoute.getRouteAction().getWeightedClusters(), Thread.currentThread().getName());
                    cluster = computeWeightCluster(xdsRoute.getRouteAction().getWeightedClusters());
                    logger.info("[XDS] Selected weighted cluster: {} [Thread: {}]", cluster, Thread.currentThread().getName());
                }
                
                if (cluster != null) {
                    logger.info("[XDS] Found cluster: {} [Thread: {}]", cluster, Thread.currentThread().getName());
                    CdsUpdate xdsCluster = xdsResourceFactory.getXdsClusterMap().get(cluster);
                    if (xdsCluster != null) {
                        logger.info("[XDS] Found CdsUpdate for cluster: {} [Thread: {}]", cluster, Thread.currentThread().getName());
                        cluster = findCluster(xdsCluster);
                        logger.info("[XDS] Final cluster after findCluster: {} [Thread: {}]", cluster, Thread.currentThread().getName());
                    } else {
                        logger.warn("[XDS] No CdsUpdate found for cluster: {} [Thread: {}]", cluster, Thread.currentThread().getName());
                    }
                }
            } else {
                logger.info("[XDS] Path did not match route [Thread: {}]", Thread.currentThread().getName());
            }
            if (cluster != null) break;
        }

        logger.info("[XDS] matchCluster returning: {} [Thread: {}]", cluster, Thread.currentThread().getName());
        return cluster;
    }

    private String findCluster(CdsUpdate xdsCluster) {
        if (ClusterType.EDS.equals(xdsCluster.getClusterType())) {
            return xdsCluster.getEdsServiceName();
        } else if (ClusterType.AGGREGATE.equals(xdsCluster.getClusterType())) {
            String cluster = xdsCluster.getPrioritizedClusterNames().get(0);
            CdsUpdate cdsUpdate = xdsResourceFactory.getXdsClusterMap().get(cluster);
            return findCluster(cdsUpdate);
        } else {
            return null;
        }
    }

    private String computeWeightCluster(List<ClusterWeight> weightedClusters) {
        int totalWeight = Math.max(
                weightedClusters.stream().mapToInt(ClusterWeight::getWeight).sum(), 1);

        // 使用System.nanoTime()作为种子来增加随机性
        long seed = System.nanoTime();
        int target = new java.util.Random(seed).nextInt(totalWeight) + 1;
        logger.info("[XDS] computeWeightCluster: totalWeight={}, target={}, seed={} [Thread: {}]", totalWeight, target, seed, Thread.currentThread().getName());
        
        int cumulativeWeight = 0;
        for (ClusterWeight xdsClusterWeight : weightedClusters) {
            int weight = xdsClusterWeight.getWeight();
            cumulativeWeight += weight;
            logger.info("[XDS] computeWeightCluster: cluster={}, weight={}, cumulativeWeight={}, target={} [Thread: {}]", 
                       xdsClusterWeight.getName(), weight, cumulativeWeight, target, Thread.currentThread().getName());
            if (target <= cumulativeWeight) {
                logger.info("[XDS] computeWeightCluster: selected cluster={} [Thread: {}]", xdsClusterWeight.getName(), Thread.currentThread().getName());
                return xdsClusterWeight.getName();
            }
        }
        logger.warn("[XDS] computeWeightCluster: no cluster selected, returning null [Thread: {}]", Thread.currentThread().getName());
        return null;
    }

    private BitList<Invoker<T>> matchInvoker(String clusterName, BitList<Invoker<T>> invokers) {
        logger.info("[XDS] matchInvoker called with clusterName: {}, invokers count: {} [Thread: {}]", clusterName, invokers.size(), Thread.currentThread().getName());
        
        if (StringUtils.isEmpty(clusterName)) {
            logger.info("[XDS] clusterName is empty, returning all invokers [Thread: {}]", Thread.currentThread().getName());
            return invokers;
        }

        for (Invoker<T> invoker : invokers) {
            String clusterID = invoker.getUrl().getParameter("clusterID");
            logger.info("[XDS] Checking invoker: {} with clusterID: {} [Thread: {}]", invoker.getUrl(), clusterID, Thread.currentThread().getName());
        }

        // Clone the original BitList to preserve the originList
        BitList<Invoker<T>> result = invokers.clone();

        // Remove non-matching invokers from the cloned list.
        // This only manipulates the internal BitSet, keeping the originList intact.
        result.removeIf(inv -> {
            String clusterID = inv.getUrl().getParameter("clusterID");
            boolean matches = clusterID != null && clusterID.equals(clusterName);
            logger.info("[XDS] Invoker {} matches cluster {}: {} [Thread: {}]", inv.getUrl(), clusterName, matches, Thread.currentThread().getName());
            // The predicate for removeIf should return true for elements to be REMOVED.
            return !matches;
        });

        logger.info("[XDS] matchInvoker returning {} invokers [Thread: {}]", result.size(), Thread.currentThread().getName());
        
        // 立即验证返回的BitList内容
        logger.info("[XDS] matchInvoker VERIFICATION: BitList size={} [Thread: {}]", result.size(), Thread.currentThread().getName());
        for (int i = 0; i < result.size(); i++) {
            Invoker<T> invoker = result.get(i);
            String clusterID = invoker.getUrl().getParameter("clusterID");
            String address = invoker.getUrl().getAddress();
            logger.info("[XDS] matchInvoker VERIFICATION invoker[{}]: {} (clusterID: {}) [Thread: {}]", i, address, clusterID, Thread.currentThread().getName());
        }
        
        return result;
    }
}
