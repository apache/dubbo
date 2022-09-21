package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcInvocation;
import org.apache.dubbo.rpc.RpcStatus;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class AdaptivePowerOfTwoChoiceTest extends LoadBalanceBaseTest{

    private Invoker<LoadBalanceBaseTest> p2cInvoker1;
    private Invoker<LoadBalanceBaseTest> p2cInvoker2;
    private Invoker<LoadBalanceBaseTest> p2cInvoker3;
    private Invoker<LoadBalanceBaseTest> p2cInvoker4;
    private Invoker<LoadBalanceBaseTest> p2cInvoker5;

    RpcStatus p2cTestRpcStatus1;
    RpcStatus p2cTestRpcStatus2;
    RpcStatus p2cTestRpcStatus3;
    RpcStatus p2cTestRpcStatus4;
    RpcStatus p2cTestRpcStatus5;

    RpcInvocation p2cTestInvocation;

    List<Invoker<LoadBalanceBaseTest>> p2cInvokers = new ArrayList<>();

    @BeforeEach
    public void setForAdaptiveP2CTest() throws Exception{
        p2cInvoker1 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        p2cInvoker2 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        p2cInvoker3 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        p2cInvoker4 = mock(Invoker.class, Mockito.withSettings().stubOnly());
        p2cInvoker5 = mock(Invoker.class, Mockito.withSettings().stubOnly());

        p2cTestInvocation = new RpcInvocation();
        p2cTestInvocation.setMethodName("test");

        URL url1 = URL.valueOf("test1://127.0.0.1:101/DemoService?weight=1&active=0");
        URL url2 = URL.valueOf("test2://127.0.0.1:102/DemoService?weight=1&active=0");
        URL url3 = URL.valueOf("test3://127.0.0.1:103/DemoService?weight=1&active=0");
        URL url4 = URL.valueOf("test4://127.0.0.1:104/DemoService?weight=1&active=0");
        URL url5 = URL.valueOf("test5://127.0.0.1:105/DemoService?weight=1&active=0");


        given(p2cInvoker1.isAvailable()).willReturn(true);
        given(p2cInvoker1.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(p2cInvoker1.getUrl()).willReturn(url1);

        given(p2cInvoker2.isAvailable()).willReturn(true);
        given(p2cInvoker2.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(p2cInvoker2.getUrl()).willReturn(url2);

        given(p2cInvoker3.isAvailable()).willReturn(true);
        given(p2cInvoker3.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(p2cInvoker3.getUrl()).willReturn(url3);

        given(p2cInvoker4.isAvailable()).willReturn(true);
        given(p2cInvoker4.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(p2cInvoker4.getUrl()).willReturn(url4);

        given(p2cInvoker5.isAvailable()).willReturn(true);
        given(p2cInvoker5.getInterface()).willReturn(LoadBalanceBaseTest.class);
        given(p2cInvoker5.getUrl()).willReturn(url5);

        p2cInvokers.add(p2cInvoker1);
        p2cInvokers.add(p2cInvoker2);
        p2cInvokers.add(p2cInvoker3);
        p2cInvokers.add(p2cInvoker4);
        p2cInvokers.add(p2cInvoker5);

        p2cTestRpcStatus1 = RpcStatus.getStatus(p2cInvoker1.getUrl(),p2cTestInvocation.getMethodName());
        p2cTestRpcStatus2 = RpcStatus.getStatus(p2cInvoker2.getUrl(),p2cTestInvocation.getMethodName());
        p2cTestRpcStatus3 = RpcStatus.getStatus(p2cInvoker3.getUrl(),p2cTestInvocation.getMethodName());
        p2cTestRpcStatus4 = RpcStatus.getStatus(p2cInvoker4.getUrl(),p2cTestInvocation.getMethodName());
        p2cTestRpcStatus5 = RpcStatus.getStatus(p2cInvoker5.getUrl(),p2cTestInvocation.getMethodName());

        RpcStatus.beginCount(p2cInvoker1.getUrl(),p2cTestInvocation.getMethodName());
        RpcStatus.beginCount(p2cInvoker2.getUrl(),p2cTestInvocation.getMethodName());
        RpcStatus.beginCount(p2cInvoker3.getUrl(),p2cTestInvocation.getMethodName());
        RpcStatus.beginCount(p2cInvoker4.getUrl(),p2cTestInvocation.getMethodName());
        RpcStatus.beginCount(p2cInvoker5.getUrl(),p2cTestInvocation.getMethodName());

        RpcStatus.endCount(p2cInvoker1.getUrl(),p2cTestInvocation.getMethodName(),1000L,true);
        RpcStatus.endCount(p2cInvoker2.getUrl(),p2cTestInvocation.getMethodName(),2000L,true);
        RpcStatus.endCount(p2cInvoker3.getUrl(),p2cTestInvocation.getMethodName(),3000L,true);
        RpcStatus.endCount(p2cInvoker4.getUrl(),p2cTestInvocation.getMethodName(),4000L,true);
        RpcStatus.endCount(p2cInvoker5.getUrl(),p2cTestInvocation.getMethodName(),5000L,true);

    }

    @Test
    public void testAdaptivePowerOfTwoChoice(){
        int run = 10000;
        Map<Invoker, AtomicLong> counter = new ConcurrentHashMap<Invoker,AtomicLong>();
        AdaptivePowerOfTwoChoice lb = (AdaptivePowerOfTwoChoice)getLoadBalance(AdaptivePowerOfTwoChoice.NAME);

        for(Invoker invoker : p2cInvokers){
            counter.put(invoker,new AtomicLong(0));
        }

        for(int i = 0; i < run; i++){
            Invoker tmpInvoker = lb.select(p2cInvokers,null,p2cTestInvocation);
            counter.get(tmpInvoker).incrementAndGet();
        }
        double[] weights = new double[5];
        double tmpAverageLatency = lb.getAverageLatency(p2cInvokers,p2cTestInvocation);
        for(int i = 0; i < 5; i++){

            weights[i] =  lb.getWeight(p2cInvokers.get(i),p2cTestInvocation,tmpAverageLatency);
            System.out.println("the weight of p2cInvoker" + (i + 1) + " is " + weights[i]);
            System.out.println("the counter of p2cInvoker" + (i + 1) + " is " + counter.get(p2cInvokers.get(i)).get());

        }
        for(int i = 1; i < 5; i++){
            Long count1 = counter.get(p2cInvokers.get(i-1)).get();
            Long count2 = counter.get(p2cInvokers.get(i)).get();
            Assertions.assertTrue(count1 > count2);
        }

    }

}
