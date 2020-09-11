package org.apache.dubbo.rpc.cluster.router.state;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.cluster.Router;

@SPI
public interface StateRouterFactory {
    /**
     * Create router.
     * Since 2.7.0, most of the time, we will not use @Adaptive feature, so it's kept only for compatibility.
     *
     * @param url url
     * @return router instance
     */
    @Adaptive("protocol")
    StateRouter getRouter(URL url);
}
