package com.alibaba.dubbo.cache.filter;

import com.alibaba.dubbo.cache.support.lru.LruCacheFactory;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author ken.lj
 * @date 2018/1/19
 */
public class CacheFilterTest {
    private static RpcInvocation invocation;
    static CacheFilter cacheFilter = new CacheFilter();
    static Invoker<?> invoker = EasyMock.createMock(Invoker.class);
    static Invoker<?> invoker1 = EasyMock.createMock(Invoker.class);
    static Invoker<?> invoker2 = EasyMock.createMock(Invoker.class);

    @BeforeClass
    public static void setUp() {
        invocation = new RpcInvocation();
        cacheFilter.setCacheFactory(new LruCacheFactory());

        URL url = URL.valueOf("test://test:11/test?cache=lru");

        EasyMock.expect(invoker.invoke(invocation)).andReturn(new RpcResult(new String("value"))).anyTimes();
        EasyMock.expect(invoker.getUrl()).andReturn(url).anyTimes();
        EasyMock.replay(invoker);

        EasyMock.expect(invoker1.invoke(invocation)).andReturn(new RpcResult(new String("value1"))).anyTimes();
        EasyMock.expect(invoker1.getUrl()).andReturn(url).anyTimes();
        EasyMock.replay(invoker1);

        EasyMock.expect(invoker2.invoke(invocation)).andReturn(new RpcResult(new String("value2"))).anyTimes();
        EasyMock.expect(invoker2.getUrl()).andReturn(url).anyTimes();
        EasyMock.replay(invoker2);
    }

    @Test
    public void test_No_Arg_Method() {
        invocation.setMethodName("echo");
        invocation.setParameterTypes(new Class<?>[]{});
        invocation.setArguments(new Object[]{});

        cacheFilter.invoke(invoker, invocation);
        RpcResult rpcResult1 = (RpcResult) cacheFilter.invoke(invoker1, invocation);
        RpcResult rpcResult2 = (RpcResult) cacheFilter.invoke(invoker2, invocation);
        Assert.assertEquals(rpcResult1.getValue(), rpcResult2.getValue());
    }

    @Test
    public void test_Args_Method() {
        invocation.setMethodName("echo1");
        invocation.setParameterTypes(new Class<?>[]{String.class});
        invocation.setArguments(new Object[]{"arg1"});

        cacheFilter.invoke(invoker, invocation);
        RpcResult rpcResult1 = (RpcResult) cacheFilter.invoke(invoker1, invocation);
        RpcResult rpcResult2 = (RpcResult) cacheFilter.invoke(invoker2, invocation);
        Assert.assertEquals(rpcResult1.getValue(), rpcResult2.getValue());
    }
}
