package org.apache.dubbo.rpc.cluster.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.model.ApplicationModel;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ERROR_INITIALIZE_XDS;

public class XdsServiceDiscovery {
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(XdsServiceDiscovery.class);

    private PilotExchanger exchanger;

    public XdsServiceDiscovery(ApplicationModel applicationModel, URL registryURL) {
        // super(applicationModel, registryURL);
        doInitialize(registryURL);
    }

    public void doInitialize(URL registryURL) {
        try {
            exchanger = PilotExchanger.initialize(registryURL);
        } catch (Throwable t) {
            logger.error(REGISTRY_ERROR_INITIALIZE_XDS, "", "", t.getMessage(), t);
        }
    }

    public void doDestroy() {
        try {
            if (exchanger == null) {
                return;
            }
            exchanger.destroy();
        } catch (Throwable t) {
            logger.error(REGISTRY_ERROR_INITIALIZE_XDS, "", "", t.getMessage(), t);
        }
    }
}
