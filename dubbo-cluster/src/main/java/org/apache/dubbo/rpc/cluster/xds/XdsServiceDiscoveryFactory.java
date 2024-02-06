package org.apache.dubbo.rpc.cluster.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_INITIALIZE_XDS;

public class XdsServiceDiscoveryFactory {

    private static final ErrorTypeAwareLogger logger =
            LoggerFactory.getErrorTypeAwareLogger(XdsServiceDiscoveryFactory.class);

    protected XdsServiceDiscovery createDiscovery(URL registryURL) {
        XdsServiceDiscovery xdsServiceDiscovery = new XdsServiceDiscovery(ApplicationModel.defaultModel(), registryURL);
        try {
            xdsServiceDiscovery.doInitialize(registryURL);
        } catch (Exception e) {
            logger.error(
                    REGISTRY_ERROR_INITIALIZE_XDS,
                    "",
                    "",
                    "Error occurred when initialize xDS service discovery impl.",
                    e);
        }
        return xdsServiceDiscovery;
    }
}
