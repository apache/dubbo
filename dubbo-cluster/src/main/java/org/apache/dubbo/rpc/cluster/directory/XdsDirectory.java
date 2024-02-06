package org.apache.dubbo.rpc.cluster.directory;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.SingleRouterChain;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.xds.PilotExchanger;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsCluster;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsClusterWeight;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsEndpoint;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsRoute;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsRouteAction;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsVirtualHost;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class XdsDirectory<T>  extends AbstractDirectory<T>{

    private final URL url;

    private final Class<T> serviceType;

    private final String applicationName;

    private final String protocolName;

    PilotExchanger pilotExchanger = PilotExchanger.getInstance();

    private Set<String> subscribeApplications;

    protected RouterChain<T> routerChain;

    protected List<Invoker<T>> invokers;
    private Protocol protocol;

    private final Map<String, XdsVirtualHost> xdsVirtualHostMap = new ConcurrentHashMap<>();

    private final Map<String, XdsCluster<T>> xdsClusterMap = new ConcurrentHashMap<>();

    public XdsDirectory(Directory<T> directory) {
        super(directory.getConsumerUrl(), true);
        this.serviceType = directory.getInterface();
        this.url = directory.getConsumerUrl();
        this.applicationName = url.getParameter("providedBy");
        this.protocolName = url.getProtocol();
        this.protocol = directory.getProtocol();
        this.routerChain = directory.getRouterChain();

        // 订阅资源
        pilotExchanger.subscribeRds(this.applicationName, this);

    }

    public Map<String, XdsVirtualHost> getXdsVirtualHostMap() {
        return xdsVirtualHostMap;
    }

    public Map<String, XdsCluster<T>> getXdsClusterMap() {
        return xdsClusterMap;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Override
    public Class<T> getInterface() {
        return serviceType;
    }

    public List<Invoker<T>> doList(SingleRouterChain<T> singleRouterChain, BitList<Invoker<T>> invokers, Invocation invocation) {
        List<Invoker<T>> result = singleRouterChain.route(this.getConsumerUrl(), invokers, invocation);
        return (List)(result == null ? BitList.emptyList() : result);
    }

    @Override
    public List<Invoker<T>> getAllInvokers() {
        return this.invokers;
    }

    public void onRdsChange(String applicationName, XdsVirtualHost xdsVirtualHost) {
        Set<String> oldCluster = getAllCluster();
        xdsVirtualHostMap.put(applicationName, xdsVirtualHost);
        Set<String> newCluster = getAllCluster();
        changeClusterSubscribe(oldCluster, newCluster);
    }

    private Set<String> getAllCluster() {
        if (CollectionUtils.isEmptyMap(xdsVirtualHostMap)) {
            return new HashSet<>();
        }
        Set<String> clusters = new HashSet<>();
        xdsVirtualHostMap.forEach((applicationName, xdsVirtualHost) -> {
            for (XdsRoute xdsRoute : xdsVirtualHost.getRoutes()) {
                XdsRouteAction action = xdsRoute.getRouteAction();
                if (action.getCluster() != null) {
                    clusters.add(action.getCluster());
                } else if (CollectionUtils.isNotEmpty(action.getClusterWeights())) {
                    for (XdsClusterWeight weightedCluster : action.getClusterWeights()) {
                        clusters.add(weightedCluster.getName());
                    }
                }
            }
        });
        return clusters;
    }

    private void changeClusterSubscribe(Set<String> oldCluster, Set<String> newCluster) {
        Set<String> removeSubscribe = new HashSet<>(oldCluster);
        Set<String> addSubscribe = new HashSet<>(newCluster);

        removeSubscribe.removeAll(newCluster);
        addSubscribe.removeAll(oldCluster);

        // remove subscribe cluster
        for (String cluster : removeSubscribe) {
            pilotExchanger.unSubscribeCds(cluster, this);
            xdsClusterMap.remove(cluster);
        }
        // add subscribe cluster
        for (String cluster : addSubscribe) {
            pilotExchanger.subscribeCds(cluster, this);
        }
    }

    public void onCdsChange(String clusterName, XdsCluster<T> xdsCluster) {
        xdsClusterMap.put(clusterName, xdsCluster);
        String lbPolicy = xdsCluster.getLbPolicy();
        List<XdsEndpoint> xdsEndpoints = xdsCluster.getXdsEndpoints();
        BitList<Invoker<T>> invokers = new BitList<>(Collections.emptyList());
        xdsEndpoints.forEach(e -> {
            String ip = e.getAddress();
            int port = e.getPortValue();
            URL url = new URL(this.protocolName, ip, port);
            // 设置clusterName 属性，说明该 invoker 属于哪个 cluster
            url.addParameter("clusterName", clusterName);
            // 设置负载均衡策略
            url.addParameter("loadbalance", lbPolicy);
            //  cluster to invoker
            Invoker<T> invoker = this.protocol.refer(this.serviceType, url);
            invokers.add(invoker);
        });
        this.invokers.addAll(invokers);
        xdsCluster.setInvokers(invokers);
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
