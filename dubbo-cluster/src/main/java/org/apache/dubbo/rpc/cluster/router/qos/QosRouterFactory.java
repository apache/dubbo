package org.apache.dubbo.rpc.cluster.router.qos;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterFactory;

@Activate
public class QosRouterFactory implements RouterFactory {

    public static final String NAME = "qos";

    @Override
    public Router getRouter(URL url) {
        return new QosRouter(url);
    }
}
