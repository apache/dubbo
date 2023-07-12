package org.apache.dubbo.rpc.cluster.spi;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.SpiMethods;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.support.registry.ZoneAwareCluster;
import org.apache.dubbo.rpc.model.ScopeModel;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.UNLOAD_CLUSTER_RELATED;

public class CreateClusterInvoker implements SpiMethod {

    @Override
    public SpiMethods methodName() {
        return SpiMethods.createClusterInvoker;
    }


    @Override
    public boolean attachToApplication() {
        return false;
    }

    /**
     * The spi method.
     *
     * @param params params
     * @return return value
     */
    @Override
    public Object invoke(Object... params) {

        List<URL> urls = (List<URL>) params[0];
        Protocol protocolSPI = (Protocol) params[1];
        Class<?> interfaceClass = (Class<?>) params[2];
        ScopeModel scopeModel = (ScopeModel) params[3];
        boolean registry = (boolean) params[4];

        Invoker<?> invoker = null;

        if (!registry) {
            URL curUrl = urls.get(0);
            invoker = protocolSPI.refer(interfaceClass, curUrl);
            // registry url, mesh-enable and unloadClusterRelated is true, not need Cluster.
            if (!UrlUtils.isRegistry(curUrl) &&
                !curUrl.getParameter(UNLOAD_CLUSTER_RELATED, false)) {
                List<Invoker<?>> invokers = new ArrayList<>();
                invokers.add(invoker);
                invoker = Cluster.getCluster(scopeModel, Cluster.DEFAULT).join(new StaticDirectory(curUrl, invokers), true);
            }
        } else {

            List<Invoker<?>> invokers = new ArrayList<>();
            URL registryUrl = null;
            for (URL url : urls) {
                // For multi-registry scenarios, it is not checked whether each referInvoker is available.
                // Because this invoker may become available later.
                invokers.add(protocolSPI.refer(interfaceClass, url));

                if (UrlUtils.isRegistry(url)) {
                    // use last registry url
                    registryUrl = url;
                }
            }

            if (registryUrl != null) {
                // registry url is available
                // for multi-subscription scenario, use 'zone-aware' policy by default
                String cluster = registryUrl.getParameter(CLUSTER_KEY, ZoneAwareCluster.NAME);
                // The invoker wrap sequence would be: ZoneAwareClusterInvoker(StaticDirectory) -> FailoverClusterInvoker
                // (RegistryDirectory, routing happens here) -> Invoker
                invoker = Cluster.getCluster(registryUrl.getScopeModel(), cluster, false).join(new StaticDirectory(registryUrl, invokers), false);
            } else {
                // not a registry url, must be direct invoke.
                if (CollectionUtils.isEmpty(invokers)) {
                    throw new IllegalArgumentException("invokers == null");
                }
                URL curUrl = invokers.get(0).getUrl();
                String cluster = curUrl.getParameter(CLUSTER_KEY, Cluster.DEFAULT);
                invoker = Cluster.getCluster(scopeModel, cluster).join(new StaticDirectory(curUrl, invokers), true);
            }
        }

        return invoker;
    }
}
