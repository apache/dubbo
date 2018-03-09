package com.alibaba.dubbo.rpc.cluster.router.unit;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.cluster.Router;
import com.alibaba.dubbo.rpc.cluster.RouterFactory;


/**
 * @author yiji.github@hotmail.com
 */
public class UnitRouterFactory  implements RouterFactory {

    public static final String NAME = "unit";

    public Router getRouter(URL url) {
        return new UnitRouter();
    }

}
