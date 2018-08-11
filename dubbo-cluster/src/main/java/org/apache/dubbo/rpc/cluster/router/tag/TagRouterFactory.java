package org.apache.dubbo.rpc.cluster.router.tag;


import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterFactory;

/**
 * @author kirito.moe@foxmail.com
 * Date 2018-08-03
 */
public class TagRouterFactory implements RouterFactory {

    public static final String NAME = "tag";

    @Override
    public Router getRouter(URL url) {
        return new TagRouter(url);
    }
}
