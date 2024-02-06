package org.apache.dubbo.rpc.cluster.xds.router;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.cluster.xds.PilotExchanger;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsCluster;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsClusterWeight;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsEndpoint;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsRoute;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsVirtualHost;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class XdsRouter<T> extends AbstractStateRouter<T> {
    private volatile URL url;

    protected ModuleModel moduleModel;

    private PilotExchanger pilotExchanger = PilotExchanger.getInstance();

    private volatile BitList<Invoker<T>> currentInvokeList;

    private Set<String> subscribeApplications;

    private Map<String, XdsVirtualHost> xdsVirtualHostMap = new ConcurrentHashMap<>();

    private Map<String, XdsCluster> xdsClusterMap = new ConcurrentHashMap<>();

    public XdsRouter(URL url) {
        super(url);
        this.moduleModel = url.getOrDefaultModuleModel();
        this.url = url;
    }

    @Override
    protected BitList<Invoker<T>> doRoute(BitList<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> routerSnapshotNodeHolder, Holder<String> messageHolder) throws RpcException {

        // 1. match cluster
        String matchCluster = matchCluster(invocation);

        // 2. match invokers
        BitList<Invoker<T>> invokerList = matchInvoker(matchCluster, invokers);

        return invokerList;
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

                // 如果是权重cluster，则进行权重分配
                if (cluster == null) {
                    cluster = computeWeightCluster(xdsRoute.getRouteAction().getClusterWeights());
                }
            }
        }

        return cluster;
    }

    private String computeWeightCluster(List<XdsClusterWeight> weightedClusters) {
        int totalWeight = Math.max(weightedClusters.stream().mapToInt(XdsClusterWeight::getWeight).sum(), 1);

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
                .filter(inv -> inv.getUrl().getParameter("clusterName").equals(clusterName))
                .collect(Collectors.toList());
        return new BitList<>(filterInvokers);

        // XdsCluster<T> xdsCluster = pilotExchanger.getXdsClusterMap().get(clusterName);
        //
        // List<XdsEndpoint> endpoints = xdsCluster.getXdsEndpoints();
        // List<Invoker<T>> filterInvokers = invokers.stream()
        //     .filter(inv -> {
        //         String host = inv.getUrl().getHost();
        //         int port = inv.getUrl().getPort();
        //         Optional<XdsEndpoint> any = endpoints.stream()
        //             .filter(end -> host.equals(end.getAddress()) && port == end.getPortValue())
        //             .findAny();
        //         return any.isPresent();
        //     })
        //     .collect(Collectors.toList());
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isForce() {
        return false;
    }
}
