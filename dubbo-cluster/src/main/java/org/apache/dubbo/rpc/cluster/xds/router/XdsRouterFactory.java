package org.apache.dubbo.rpc.cluster.xds.router;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory;

@Activate(order = 100)
public class XdsRouterFactory implements StateRouterFactory {

    @Override
    public <T> StateRouter<T> getRouter(Class<T> interfaceClass, URL url) {
        return new XdsRouter<>(url);
    }
}
