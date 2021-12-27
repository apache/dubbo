package org.apache.dubbo.rpc.cluster.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.apache.dubbo.rpc.cluster.directory.StaticDirectory;
import org.apache.dubbo.rpc.cluster.filter.DemoService;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.apache.dubbo.rpc.cluster.Constants.CLUSTER_AVAILABLE_CHECK_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.INVOCATION_NEED_MOCK;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class BroacastCluster2Test {

    List<Invoker<BroacastCluster2Test.IHelloService>> invokers = new ArrayList<Invoker<BroacastCluster2Test.IHelloService>>();
    List<Invoker<BroacastCluster2Test.IHelloService>> selectedInvokers = new ArrayList<Invoker<BroacastCluster2Test.IHelloService>>();
    AbstractClusterInvoker<BroacastCluster2Test.IHelloService> cluster;
    AbstractClusterInvoker<BroacastCluster2Test.IHelloService> cluster_nocheck;
    StaticDirectory<BroacastCluster2Test.IHelloService> dic;
    RpcInvocation invocation = new RpcInvocation();
    URL url = URL.valueOf("registry://localhost:9090/org.apache.dubbo.rpc.cluster.support.BroacastCluster2Test.IHelloService?refer=" + URL.encode("application=BroacastCluster2Test"));
    URL consumerUrl = URL.valueOf("dubbo://localhost:9090?application=BroacastCluster2Test&category=routers&cluster=broadcast2&generic=true&interface=org.apache.dubbo.rpc.cluster.support.BroacastCluster2Test.IHelloService&metadata-type=remote");

    Invoker<BroacastCluster2Test.IHelloService> invoker1;
    Invoker<BroacastCluster2Test.IHelloService> invoker2;
    Invoker<BroacastCluster2Test.IHelloService> invoker3;
    Invoker<BroacastCluster2Test.IHelloService> invoker4;
    Invoker<BroacastCluster2Test.IHelloService> invoker5;
    Invoker<BroacastCluster2Test.IHelloService> mockedInvoker1;


    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterEach
    public void teardown() throws Exception {
        RpcContext.getContext().clearAttachments();
    }

    @SuppressWarnings({"unchecked"})
    @BeforeEach
    public void setUp() throws Exception {
        invocation.setMethodName("sayHello");

        invoker1 = mock(Invoker.class);
        invoker2 = mock(Invoker.class);
        invoker3 = mock(Invoker.class);
        invoker4 = mock(Invoker.class);
        invoker5 = mock(Invoker.class);
        mockedInvoker1 = mock(Invoker.class);

        URL turl = URL.valueOf("test://test:11/test");

        given(invoker1.isAvailable()).willReturn(false);
        given(invoker1.getInterface()).willReturn(BroacastCluster2Test.IHelloService.class);
        given(invoker1.getUrl()).willReturn(turl.setPort(1).addParameter("name", "invoker1"));

        given(invoker2.isAvailable()).willReturn(true);
        given(invoker2.getInterface()).willReturn(BroacastCluster2Test.IHelloService.class);
        given(invoker2.getUrl()).willReturn(turl.setPort(2).addParameter("name", "invoker2"));

        given(invoker3.isAvailable()).willReturn(false);
        given(invoker3.getInterface()).willReturn(BroacastCluster2Test.IHelloService.class);
        given(invoker3.getUrl()).willReturn(turl.setPort(3).addParameter("name", "invoker3"));

        given(invoker4.isAvailable()).willReturn(true);
        given(invoker4.getInterface()).willReturn(BroacastCluster2Test.IHelloService.class);
        given(invoker4.getUrl()).willReturn(turl.setPort(4).addParameter("name", "invoker4"));

        given(invoker5.isAvailable()).willReturn(false);
        given(invoker5.getInterface()).willReturn(BroacastCluster2Test.IHelloService.class);
        given(invoker5.getUrl()).willReturn(turl.setPort(5).addParameter("name", "invoker5"));

        given(mockedInvoker1.isAvailable()).willReturn(false);
        given(mockedInvoker1.getInterface()).willReturn(BroacastCluster2Test.IHelloService.class);
        given(mockedInvoker1.getUrl()).willReturn(turl.setPort(999).setProtocol("mock"));

        invokers.add(invoker1);
        dic = new StaticDirectory<>(url, invokers, null);
        dic.setConsumerUrl(consumerUrl);
        cluster = new AbstractClusterInvoker(dic) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                return null;
            }
        };

        cluster_nocheck = new AbstractClusterInvoker(dic, url.addParameterIfAbsent(CLUSTER_AVAILABLE_CHECK_KEY, Boolean.FALSE.toString())) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                return null;
            }
        };

    }

    public static interface IHelloService {
    }

    private void initlistsize5() {
        invokers.clear();
        selectedInvokers.clear();//Clear first, previous test case will make sure that the right invoker2 will be used.
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        invokers.add(invoker4);
        invokers.add(invoker5);
    }

    private void initDic() {
        dic.buildRouterChain();
    }


    @Test()
    public void testTimeoutExceptionCode() {
        List<Invoker<DemoService>> invokers = new ArrayList<Invoker<DemoService>>();
        invokers.add(new Invoker<DemoService>() {

            @Override
            public Class<DemoService> getInterface() {
                return DemoService.class;
            }

            public URL getUrl() {
                return URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/" + DemoService.class.getName());
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public Result invoke(Invocation invocation) throws RpcException {
                throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "test timeout");
            }

            @Override
            public void destroy() {
            }
        });
        Directory<DemoService> directory = new StaticDirectory<DemoService>(invokers);
        FailoverClusterInvoker<DemoService> failoverClusterInvoker = new FailoverClusterInvoker<DemoService>(directory);
        try {
            failoverClusterInvoker.invoke(new RpcInvocation("sayHello", DemoService.class.getName(), "", new Class<?>[0], new Object[0]));
            Assertions.fail();
        } catch (RpcException e) {
            Assertions.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
        ForkingClusterInvoker<DemoService> forkingClusterInvoker = new ForkingClusterInvoker<DemoService>(directory);
        try {
            forkingClusterInvoker.invoke(new RpcInvocation("sayHello", DemoService.class.getName(), "", new Class<?>[0], new Object[0]));
            Assertions.fail();
        } catch (RpcException e) {
            Assertions.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
        FailfastClusterInvoker<DemoService> failfastClusterInvoker = new FailfastClusterInvoker<DemoService>(directory);
        try {
            failfastClusterInvoker.invoke(new RpcInvocation("sayHello", DemoService.class.getName(), "", new Class<?>[0], new Object[0]));
            Assertions.fail();
        } catch (RpcException e) {
            Assertions.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
    }

    /**
     * Test mock invoker selector works as expected
     */
    @Test
    public void testMockedInvokerSelect() {
        initlistsize5();
        invokers.add(mockedInvoker1);

        initDic();

        RpcInvocation mockedInvocation = new RpcInvocation();
        mockedInvocation.setMethodName("sayHello");
        mockedInvocation.setAttachment(INVOCATION_NEED_MOCK, "true");
        List<Invoker<BroacastCluster2Test.IHelloService>> mockedInvokers = dic.list(mockedInvocation);
        Assertions.assertEquals(1, mockedInvokers.size());

        List<Invoker<BroacastCluster2Test.IHelloService>> invokers = dic.list(invocation);
        Assertions.assertEquals(5, invokers.size());
    }
}
