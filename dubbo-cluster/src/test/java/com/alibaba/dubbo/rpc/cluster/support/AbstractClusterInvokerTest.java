/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.cluster.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.cluster.Directory;
import com.alibaba.dubbo.rpc.cluster.LoadBalance;
import com.alibaba.dubbo.rpc.cluster.directory.StaticDirectory;
import com.alibaba.dubbo.rpc.cluster.filter.DemoService;
import com.alibaba.dubbo.rpc.cluster.loadbalance.LeastActiveLoadBalance;
import com.alibaba.dubbo.rpc.cluster.loadbalance.RandomLoadBalance;
import com.alibaba.dubbo.rpc.cluster.loadbalance.RoundRobinLoadBalance;
import com.alibaba.dubbo.rpc.support.MockProtocol;

/**
 * AbstractClusterInvokerTest
 * @author chao.liuc
 *
 */
@SuppressWarnings("rawtypes")
public class AbstractClusterInvokerTest {
    List<Invoker<IHelloService>> invokers = new ArrayList<Invoker<IHelloService>>();
    List<Invoker<IHelloService>> selectedInvokers = new ArrayList<Invoker<IHelloService>>();
    AbstractClusterInvoker<IHelloService> cluster;
    AbstractClusterInvoker<IHelloService> cluster_nocheck;
    Directory<IHelloService> dic ;
    RpcInvocation invocation = new RpcInvocation();
    URL url = URL.valueOf("registry://localhost:9090");
    
    Invoker<IHelloService> invoker1 ;
    Invoker<IHelloService> invoker2 ;
    Invoker<IHelloService> invoker3 ;
    Invoker<IHelloService> invoker4 ;
    Invoker<IHelloService> invoker5 ;
    Invoker<IHelloService> mockedInvoker1 ;
    

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }
    @SuppressWarnings({ "unchecked" })
    @Before
    public void setUp() throws Exception {
    	invocation.setMethodName("sayHello");
        
        invoker1 = EasyMock.createMock(Invoker.class);
        invoker2 = EasyMock.createMock(Invoker.class);
        invoker3 = EasyMock.createMock(Invoker.class);
        invoker4 = EasyMock.createMock(Invoker.class);
        invoker5 = EasyMock.createMock(Invoker.class);
        mockedInvoker1 = EasyMock.createMock(Invoker.class);
        
        URL turl = URL.valueOf("test://test:11/test");
        
        EasyMock.expect(invoker1.isAvailable()).andReturn(false).anyTimes();
        EasyMock.expect(invoker1.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(invoker1.getUrl()).andReturn(turl.addParameter("name", "invoker1")).anyTimes();
        
        EasyMock.expect(invoker2.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker2.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(invoker2.getUrl()).andReturn(turl.addParameter("name", "invoker2")).anyTimes();
        
        EasyMock.expect(invoker3.isAvailable()).andReturn(false).anyTimes();
        EasyMock.expect(invoker3.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(invoker3.getUrl()).andReturn(turl.addParameter("name", "invoker3")).anyTimes();
        
        EasyMock.expect(invoker4.isAvailable()).andReturn(true).anyTimes();
        EasyMock.expect(invoker4.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(invoker4.getUrl()).andReturn(turl.addParameter("name", "invoker4")).anyTimes();
        
        EasyMock.expect(invoker5.isAvailable()).andReturn(false).anyTimes();
        EasyMock.expect(invoker5.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(invoker5.getUrl()).andReturn(turl.addParameter("name", "invoker5")).anyTimes();
        
        EasyMock.expect(mockedInvoker1.isAvailable()).andReturn(false).anyTimes();
        EasyMock.expect(mockedInvoker1.getInterface()).andReturn(IHelloService.class).anyTimes();
        EasyMock.expect(mockedInvoker1.getUrl()).andReturn(turl.setProtocol("mock")).anyTimes();
        
        EasyMock.replay(invoker1,invoker2,invoker3,invoker4,invoker5,mockedInvoker1);
        
        invokers.add(invoker1);
        dic = new StaticDirectory<IHelloService>(url, invokers, null);
        cluster = new AbstractClusterInvoker(dic) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                return null;
            }
        };
        
        cluster_nocheck = new AbstractClusterInvoker(dic,url.addParameterIfAbsent(Constants.CLUSTER_AVAILABLE_CHECK_KEY, Boolean.FALSE.toString())) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
                return null;
            }
        };
        
    }
    
    @Test
    public void testSelect_Invokersize0() throws Exception {
        {
            Invoker invoker = cluster.select(null,null,null,null);
            Assert.assertEquals(null, invoker);
        }
        {
            invokers.clear();
            selectedInvokers.clear();
            Invoker invoker = cluster.select(null,null,invokers,null);
            Assert.assertEquals(null, invoker);
        }
    }
    
    @Test
    public void testSelect_Invokersize1() throws Exception {
        invokers.clear();
        invokers.add(invoker1);
        Invoker invoker = cluster.select(null,null,invokers,null);
        Assert.assertEquals(invoker1, invoker);
    }
    
    @Test
    public void testSelect_Invokersize2AndselectNotNull() throws Exception {
        invokers.clear();
        invokers.add(invoker1);
        invokers.add(invoker2);
        {
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            Invoker invoker = cluster.select(null,null,invokers,selectedInvokers);
            Assert.assertEquals(invoker2, invoker);
        }
        {
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            Invoker invoker = cluster.select(null,null,invokers,selectedInvokers);
            Assert.assertEquals(invoker1, invoker);
        }
    }
    
    @Test
    public void testSelect_multiInvokers() throws Exception {
        testSelect_multiInvokers( RoundRobinLoadBalance.NAME);
        testSelect_multiInvokers( LeastActiveLoadBalance.NAME);
        testSelect_multiInvokers( RandomLoadBalance.NAME);
    }
    
    @Test
    public void testCloseAvailablecheck(){
        LoadBalance lb = EasyMock.createMock(LoadBalance.class);
        EasyMock.expect(lb.select(invokers, invocation)).andReturn(invoker1);
        EasyMock.replay(lb);
        initlistsize5();
        
        Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
        Assert.assertEquals(false,sinvoker.isAvailable());
        Assert.assertEquals(invoker1,sinvoker);
        
    }
    
    @Test
    public void testDonotSelectAgainAndNoCheckAvailable(){
        
        LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(RoundRobinLoadBalance.NAME);
        initlistsize5();
        {
            //边界测试.
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertSame(invoker1, sinvoker);
        }
        {
            //边界测试.
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertSame(invoker2, sinvoker);
        }
        {
            //边界测试.
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers );
            Assert.assertSame(invoker3, sinvoker);
        }
        {
            //边界测试.
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers );
            Assert.assertSame(invoker5, sinvoker);
        }
        {
            //边界测试.
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster_nocheck.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertTrue(invokers.contains(sinvoker));
        }
        
    }
    
    @Test
    public void testSelectAgainAndCheckAvailable(){
        
        LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(RoundRobinLoadBalance.NAME);
        initlistsize5();
        {
            //边界测试.
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertTrue(sinvoker == invoker4 );
        }
        {
            //边界测试.
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker4);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
        }
        {
            //边界测试.
            for(int i=0;i<100;i++){
                selectedInvokers.clear();
                Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
                Assert.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
            }
        }
        {
            //边界测试.
            for(int i=0;i<100;i++){
                selectedInvokers.clear();
                selectedInvokers.add(invoker1);
                selectedInvokers.add(invoker3);
                selectedInvokers.add(invoker5);
                Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
                Assert.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
            }
        }
        {
            //边界测试.
            for(int i=0;i<100;i++){
                selectedInvokers.clear();
                selectedInvokers.add(invoker1);
                selectedInvokers.add(invoker3);
                selectedInvokers.add(invoker2);
                selectedInvokers.add(invoker4);
                selectedInvokers.add(invoker5);
                Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
                Assert.assertTrue(sinvoker == invoker2 || sinvoker == invoker4);
            }
        }
    }
    
    
    public void testSelect_multiInvokers(String lbname) throws Exception {
        
        int min=1000,max=5000;
        Double d =  (Math.random()*(max-min+1)+min);
        int runs =  d.intValue();
        Assert.assertTrue(runs>min);
        LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(lbname);
        initlistsize5();
        for(int i=0;i<runs;i++){
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true,sinvoker.isAvailable());
        }
        for(int i=0;i<runs;i++){
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true,sinvoker.isAvailable());
        }
        for(int i=0;i<runs;i++){
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true,sinvoker.isAvailable());
        }
        for(int i=0;i<runs;i++){
            selectedInvokers.clear();
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker4);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true,sinvoker.isAvailable());
        }
        for(int i=0;i<runs;i++){
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker3);
            selectedInvokers.add(invoker5);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true,sinvoker.isAvailable());
        }
        for(int i=0;i<runs;i++){
            selectedInvokers.clear();
            selectedInvokers.add(invoker1);
            selectedInvokers.add(invoker2);
            selectedInvokers.add(invoker3);
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            Assert.assertEquals(true,sinvoker.isAvailable());
        }
    }

    /**
     * 测试均衡.
     */
    @Test
    public void testSelectBalance(){
        
        LoadBalance lb = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(RoundRobinLoadBalance.NAME);
        initlistsize5();
        
        Map<Invoker,AtomicLong> counter = new ConcurrentHashMap<Invoker,AtomicLong>();
        for(Invoker invoker :invokers){
            counter.put(invoker, new AtomicLong(0));
        }
        int runs = 1000;
        for(int i=0;i<runs;i++){
            selectedInvokers.clear();
            Invoker sinvoker = cluster.select(lb, invocation, invokers, selectedInvokers);
            counter.get(sinvoker).incrementAndGet();
        }
        
        for (Invoker minvoker :counter.keySet() ){
            Long count = counter.get(minvoker).get();
//            System.out.println(count);
            if(minvoker.isAvailable())
                Assert.assertTrue("count should > avg", count>runs/invokers.size());
        }
        
        Assert.assertEquals(runs, counter.get(invoker2).get()+counter.get(invoker4).get());;
        
    }
    
    private void initlistsize5(){
        invokers.clear();
        selectedInvokers.clear();//需要清除，之前的测试中会主动将正确的invoker2放入其中.
        invokers.add(invoker1);
        invokers.add(invoker2);
        invokers.add(invoker3);
        invokers.add(invoker4);
        invokers.add(invoker5);
    }
    @Test()
    public void testTimeoutExceptionCode() {
        List<Invoker<DemoService>> invokers = new ArrayList<Invoker<DemoService>>();
        invokers.add(new Invoker<DemoService>() {

            public Class<DemoService> getInterface() {
                return DemoService.class;
            }

            public URL getUrl() {
                return URL.valueOf("dubbo://" + NetUtils.getLocalHost() + ":20880/" + DemoService.class.getName());
            }

            public boolean isAvailable() {
                return false;
            }

            public Result invoke(Invocation invocation) throws RpcException {
                throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "test timeout");
            }

            public void destroy() {
            }
        });
        Directory<DemoService> directory = new StaticDirectory<DemoService>(invokers);
        FailoverClusterInvoker<DemoService> failoverClusterInvoker = new FailoverClusterInvoker<DemoService>(directory);
        try {
            failoverClusterInvoker.invoke(new RpcInvocation("sayHello", new Class<?>[0], new Object[0]));
            Assert.fail();
        } catch (RpcException e) {
            Assert.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
        ForkingClusterInvoker<DemoService> forkingClusterInvoker = new ForkingClusterInvoker<DemoService>(directory);
        try {
            forkingClusterInvoker.invoke(new RpcInvocation("sayHello", new Class<?>[0], new Object[0]));
            Assert.fail();
        } catch (RpcException e) {
            Assert.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
        FailfastClusterInvoker<DemoService> failfastClusterInvoker = new FailfastClusterInvoker<DemoService>(directory);
        try {
            failfastClusterInvoker.invoke(new RpcInvocation("sayHello", new Class<?>[0], new Object[0]));
            Assert.fail();
        } catch (RpcException e) {
            Assert.assertEquals(RpcException.TIMEOUT_EXCEPTION, e.getCode());
        }
    }
	/**
	 * 测试mock invoker选择是否正常
	 */
	@Test
	public void testMockedInvokerSelect() {
		initlistsize5();
		invokers.add(mockedInvoker1);
		
		RpcInvocation mockedInvocation = new RpcInvocation();
		mockedInvocation.setMethodName("sayHello");
		mockedInvocation.setAttachment(Constants.INVOCATION_NEED_MOCK, "true");
		List<Invoker<IHelloService>> mockedInvokers = dic.list(mockedInvocation);
		Assert.assertEquals(1, mockedInvokers.size());
		
		List<Invoker<IHelloService>> invokers = dic.list(invocation);
		Assert.assertEquals(5, invokers.size());
	}
	
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerInvoke_normal(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName());
		url = url.addParameter(Constants.MOCK_KEY, "fail" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
        URL mockUrl = URL.valueOf("mock://localhost/"+IHelloService.class.getName()
				+"?getSomething.mock=return aa");
		
		Protocol protocol = new MockProtocol();
		Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
		invokers.add(mInvoker1);
        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("something", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assert.assertEquals(null, ret.getValue());
	}
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerInvoke_failmock(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter(Constants.MOCK_KEY, "fail:return null" )
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
        URL mockUrl = URL.valueOf("mock://localhost/"+IHelloService.class.getName()
				+"?getSomething.mock=return aa").addParameters(url.getParameters());
		
		Protocol protocol = new MockProtocol();
		Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
		invokers.add(mInvoker1);
        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("aa", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assert.assertEquals(null, ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assert.assertEquals(null, ret.getValue());
	}
	
	
	/**
	 * 测试mock策略是否正常-force-mork
	 */
	@Test
	public void testMockInvokerInvoke_forcemock(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName());
		url = url.addParameter(Constants.MOCK_KEY, "force:return null" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
	    URL mockUrl = URL.valueOf("mock://localhost/"+IHelloService.class.getName()
				+"?getSomething.mock=return aa&getSomething3xx.mock=return xx")
				.addParameters(url.getParameters());
		
		Protocol protocol = new MockProtocol();
		Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
		invokers.add(mInvoker1);
	    
		//方法配置了mock
	    RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
	    Result ret = cluster.invoke(invocation);
	    Assert.assertEquals("aa", ret.getValue());
	    
	  //如果没有配置mock，则直接返回null
	    invocation = new RpcInvocation();
		invocation.setMethodName("getSomething2");
	    ret = cluster.invoke(invocation);
	    Assert.assertEquals(null, ret.getValue());
	    
	    //如果没有配置mock，则直接返回null
	    invocation = new RpcInvocation();
		invocation.setMethodName("sayHello");
	    ret = cluster.invoke(invocation);
	    Assert.assertEquals(null, ret.getValue());
	}
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerFromOverride_Invoke_Fock_someMethods(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getSomething.mock","fail:return x")
				.addParameter("getSomething2.mock","force:return y");
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("something", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("y", ret.getValue());
        
      //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething3");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("something3", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assert.assertEquals(null, ret.getValue());
	}
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerFromOverride_Invoke_Fock_WithOutDefault(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getSomething.mock","fail:return x")
				.addParameter("getSomething2.mock","force:return y")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("x", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("y", ret.getValue());
        
      //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething3");
		try {
			ret = cluster.invoke(invocation);
			Assert.fail();
		}catch (RpcException e) {
			
		}
	}
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerFromOverride_Invoke_Fock_WithDefault(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("mock","fail:return null")
				.addParameter("getSomething.mock","fail:return x")
				.addParameter("getSomething2.mock","force:return y")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("x", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("y", ret.getValue());
        
      //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething3");
        ret = cluster.invoke(invocation);
        Assert.assertEquals(null, ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assert.assertEquals(null, ret.getValue());
	}
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerFromOverride_Invoke_Fock_WithFailDefault(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("mock","fail:return z")
				.addParameter("getSomething.mock","fail:return x")
				.addParameter("getSomething2.mock","force:return y")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("x", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("y", ret.getValue());
        
      //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething3");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("z", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("z", ret.getValue());
	}
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerFromOverride_Invoke_Fock_WithForceDefault(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("mock","force:return z")
				.addParameter("getSomething.mock","fail:return x")
				.addParameter("getSomething2.mock","force:return y")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("x", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("y", ret.getValue());
        
      //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething3");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("z", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("z", ret.getValue());
	}
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerFromOverride_Invoke_Fock_Default(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("mock","fail:return x")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("x", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething2");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("x", ret.getValue());
        
      //如d
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("sayHello");
        ret = cluster.invoke(invocation);
        Assert.assertEquals("x", ret.getValue());
	}
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerFromOverride_Invoke_checkCompatible_return(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getSomething.mock","return x")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("x", ret.getValue());
        
        //如果没有配置mock，则直接返回null
        invocation = new RpcInvocation();
		invocation.setMethodName("getSomething3");
		try{
			ret = cluster.invoke(invocation);
			Assert.fail("fail invoke");
		}catch(RpcException e){
			
		}
	}
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("mock","true")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("somethingmock", ret.getValue());
	}
	
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock2(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("mock","fail")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("somethingmock", ret.getValue());
	}
	/**
	 * 测试mock策略是否正常-fail-mock
	 */
	@Test
	public void testMockInvokerFromOverride_Invoke_checkCompatible_ImplMock3(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("mock","force");
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals("somethingmock", ret.getValue());
	}
	
	@Test
	public void testMockInvokerFromOverride_Invoke_check_int(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getInt1.mock","force:return 1688")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getInt1");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals(1688, Integer.parseInt(ret.getValue().toString()));
	}
	
	@Test
	public void testMockInvokerFromOverride_Invoke_check_boolean(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getBoolean1.mock","force:return true")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getBoolean1");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals(true, Boolean.parseBoolean(ret.getValue().toString()));
	}
	
	@Test
	public void testMockInvokerFromOverride_Invoke_check_Boolean(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getBoolean2.mock","force:return true")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getBoolean2");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals(true, Boolean.parseBoolean(ret.getValue().toString()));
	}
	
	@Test
	public void testMockInvokerFromOverride_Invoke_force_throw(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getBoolean2.mock","force:throw ")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getBoolean2");
		try {
			cluster.invoke(invocation);
			Assert.fail();
		} catch (RpcException e) {
			Assert.assertTrue(e.isMock());
		}
	}
	
	
	@SuppressWarnings("unchecked")
	private Invoker<IHelloService> getClusterInvoker(URL url){
		//javasssit方式对方法参数类型判断严格,如果invocation数据设置不全，调用会失败.
		final URL durl = url.addParameter("proxy", "jdk");
		invokers.clear();
		ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension("jdk");
		Invoker<IHelloService> invoker1 = proxy.getInvoker(new HelloService(), IHelloService.class, durl);
		invokers.add(invoker1);
		
		Directory<IHelloService> dic = new StaticDirectory<IHelloService>(durl, invokers, null);
		AbstractClusterInvoker<IHelloService> cluster = new AbstractClusterInvoker(dic) {
            @Override
            protected Result doInvoke(Invocation invocation, List invokers, LoadBalance loadbalance)
                    throws RpcException {
            	if (durl.getParameter("invoke_return_error", false)){
            		throw new RpcException("test rpc exception");
            	} else {
            		return ((Invoker<?>)invokers.get(0)).invoke(invocation);
            	}
            }
        };
        return cluster;
	}
	
//	@Test
	public void testJavassist(){
		ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension("javassist");
		Invoker<IHelloService> invoker1 = proxy.getInvoker(new HelloService(), IHelloService.class, url);
		RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
		invocation.setParameterTypes(new Class<?>[]{});
		invocation.setArguments(new Object[]{});
		Result result = invoker1.invoke(invocation);
		System.out.println(result.getValue());
	}
	
	public static interface IHelloService{
		String getSomething();
		String getSomething2();
		String getSomething3();
		int getInt1();
		boolean getBoolean1();
		Boolean getBoolean2();
		void sayHello();
	}
	public static class HelloService implements IHelloService {
		public String getSomething() {
			return "something";
		}
		public String getSomething2() {
			return "something2";
		}
		public String getSomething3() {
			return "something3";
		}
		public int getInt1() {
			return 1;
		}
		public boolean getBoolean1() {
			return false;
		}
		public Boolean getBoolean2() {
			return Boolean.FALSE;
		}
		public void sayHello() {
			System.out.println("hello prety");
		}
	}
	
	public static class IHelloServiceMock implements IHelloService {
		public IHelloServiceMock() {
			
		}
		public String getSomething() {
			return "somethingmock";
		}
		public String getSomething2() {
			return "something2mock";
		}
		public String getSomething3() {
			return "something3mock";
		}
		public int getInt1() {
			return 1;
		}
		public boolean getBoolean1() {
			return false;
		}
		public Boolean getBoolean2() {
			return Boolean.FALSE;
		}
		public void sayHello() {
			System.out.println("hello prety");
		}
	}
	
}