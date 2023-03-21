package org.apache.dubbo.rpc.cluster.router.expression;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterFactory;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.cluster.router.state.StateRouterFactory;

@Activate
public class ExpressionRouterFactory implements StateRouterFactory {

    public static final String NAME = "expression";

    @Override
    public <T> StateRouter<T> getRouter(Class<T> interfaceClass, URL url) {
        return new ExpressionRouter(url);
    }
}
