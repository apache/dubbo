package org.apache.dubbo.rpc.cluster.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterChain;
import org.apache.dubbo.rpc.cluster.SingleRouterChain;
import org.apache.dubbo.rpc.cluster.directory.XdsDirectory;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsCluster;
import org.apache.dubbo.rpc.cluster.xds.resource.XdsVirtualHost;
import org.apache.dubbo.rpc.cluster.xds.router.XdsRouter;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Arrays;
import java.util.Map;
import static org.mockito.Mockito.when;

public class DemoTest {

    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();

    private ProxyFactory proxy =
            ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    @Test
    public void testXdsRouterInitial() {

        URL url = URL.valueOf("xds://localhost:15010/?secure=plaintext");

        PilotExchanger.initialize(url);

        Directory directory = Mockito.spy(Directory.class);
        when(directory.getConsumerUrl()).thenReturn(URL.valueOf("dubbo://0.0.0.0:15010/DemoService?providedBy=dubbo-samples-xds-provider"));
        when(directory.getInterface()).thenReturn(DemoService.class);
        when(directory.getProtocol()).thenReturn(protocol);
        SingleRouterChain singleRouterChain = new SingleRouterChain<>(null, Arrays.asList(new XdsRouter<>(url)), false, null);
        RouterChain routerChain = new RouterChain<>(new SingleRouterChain[] {singleRouterChain});
        when(directory.getRouterChain()).thenReturn(routerChain);

        XdsDirectory xdsDirectory = new XdsDirectory(directory);

        Invocation invocation = Mockito.mock(Invocation.class);
        Invoker invoker = Mockito.mock(Invoker.class);
        URL url1 = URL.valueOf("consumer://0.0.0.0:15010/DemoService?providedBy=dubbo-samples-xds-provider");
        when(invoker.getUrl()).thenReturn(url1);
        when(invocation.getInvoker()).thenReturn(invoker);

        while(true) {
            Map<String, XdsVirtualHost> xdsVirtualHostMap = xdsDirectory.getXdsVirtualHostMap();
            Map<String, XdsCluster> xdsClusterMap = xdsDirectory.getXdsClusterMap();
            if (!xdsVirtualHostMap.isEmpty() && !xdsClusterMap.isEmpty()) {
                // xdsRouterDemo.route(invokers, url, invocation, false, null);
                xdsDirectory.list(invocation);
            }
            Thread.yield();
        }
    }

    private Invoker<Object> createInvoker(String app, String address) {
        URL url = URL.valueOf("dubbo://" + address + "/DemoInterface?"
                + (StringUtils.isEmpty(app) ? "" : "remote.application=" + app));
        Invoker invoker = Mockito.mock(Invoker.class);
        when(invoker.getUrl()).thenReturn(url);
        return invoker;
    }

}
