package org.apache.dubbo.rpc.cluster.spi;

import org.apache.dubbo.common.constants.SpiMethodNames;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.deploy.lifecycle.SpiMethod;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;

import java.util.Arrays;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.config.utils.ConfigValidationUtils.checkExtension;
import static org.apache.dubbo.config.utils.ConfigValidationUtils.checkMultiExtension;

public class CheckClusterExtension implements SpiMethod {

    @Override
    public SpiMethodNames methodName() {
        return SpiMethodNames.checkClusterExtension;
    }

    @Override
    public boolean attachToApplication() {
        return false;
    }

    /**
     * Check if cluster related extensions exist. If no implement of {@link Cluster} and {@link ClusterFilter} , or {@link LoadBalance} found, an exception will throw.
     */
    @Override
    public Object invoke(Object... params) {

        AbstractConfig config = (AbstractConfig) params[0];

        if(config instanceof AbstractInterfaceConfig) {

            checkExtension(config.getScopeModel(), Cluster.class, CLUSTER_KEY, ((AbstractInterfaceConfig)config).getCluster());
            checkMultiExtension(config.getScopeModel(), Arrays.asList(Filter.class, ClusterFilter.class), FILTER_KEY, ((AbstractInterfaceConfig)config).getFilter());

        }else if(config instanceof MethodConfig){

            checkExtension(((MethodConfig)config).getScopeModel(), LoadBalance.class, LOADBALANCE_KEY, ((MethodConfig)config).getLoadbalance());
        }
        return null;
    }
}
