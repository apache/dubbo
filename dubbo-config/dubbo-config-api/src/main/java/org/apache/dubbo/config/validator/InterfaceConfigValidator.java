package org.apache.dubbo.config.validator;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.AbstractInterfaceConfig;
import org.apache.dubbo.config.MethodConfig;
import org.apache.dubbo.config.context.ConfigValidator;
import org.apache.dubbo.config.util.ConfigValidationUtils;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;

import java.util.Arrays;
import java.util.List;

import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.FILTER_KEY;
import static org.apache.dubbo.config.Constants.LAYER_KEY;
import static org.apache.dubbo.rpc.Constants.LOCAL_KEY;
import static org.apache.dubbo.rpc.Constants.PROXY_KEY;

@Activate
public class InterfaceConfigValidator implements ConfigValidator<AbstractInterfaceConfig> {

    public static void validateAbstractInterfaceConfig(AbstractInterfaceConfig config) {
        ConfigValidationUtils.checkName(LOCAL_KEY, config.getLocal());
        ConfigValidationUtils.checkName("stub", config.getStub());
        ConfigValidationUtils.checkMultiName("owner", config.getOwner());

        ConfigValidationUtils.checkExtension(config.getScopeModel(), ProxyFactory.class, PROXY_KEY, config.getProxy());
        ConfigValidationUtils.checkExtension(config.getScopeModel(), Cluster.class, CLUSTER_KEY, config.getCluster());
        ConfigValidationUtils.checkMultiExtension(config.getScopeModel(), Arrays.asList(Filter.class, ClusterFilter.class), FILTER_KEY, config.getFilter());
        ConfigValidationUtils.checkNameHasSymbol(LAYER_KEY, config.getLayer());

        List<MethodConfig> methods = config.getMethods();
        if (CollectionUtils.isNotEmpty(methods)) {
            methods.forEach(MethodConfig::validate);
        }
    }

    @Override
    public void validate(AbstractInterfaceConfig config) {
        validateAbstractInterfaceConfig(config);
    }

    @Override
    public boolean isSupport(Class<?> configClass) {
        return AbstractInterfaceConfig.class.equals(configClass);
    }
}
