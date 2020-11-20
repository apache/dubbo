package org.apache.dubbo.rpc.cluster.router;

import java.util.ArrayList;
import java.util.List;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.router.state.StateRouter;
import org.apache.dubbo.rpc.cluster.router.tag.TagDynamicStateRouter;

public class StateRouterTest {
    public static void main(String[] args) {
        List<Invoker> list = mockInvokers();

        RouterChain chain = new RouterChain(list);
        List<StateRouter> routers = new ArrayList<>();
        routers.add(new TagDynamicStateRouter(null));
        chain.addStateRouters(routers);

        URL url = mockURL();
        Invocation invocation = mockInvocation();
        long start = System.currentTimeMillis();
        chain.route(url, invocation);
        System.out.println("cost:" + (System.currentTimeMillis() - start));
    }

    private static List<Invoker> mockInvokers() {
        List<Invoker> list = new ArrayList<>();



        return list;
    }

    private static URL mockURL() {

        return null;
    }

    private static Invocation mockInvocation() {

        return null;
    }
}
