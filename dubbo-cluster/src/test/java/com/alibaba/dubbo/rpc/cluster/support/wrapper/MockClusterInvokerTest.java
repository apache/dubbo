package com.alibaba.dubbo.rpc.cluster.support.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
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
import com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker;
import com.alibaba.dubbo.rpc.support.MockProtocol;

public class MockClusterInvokerTest {
	
	List<Invoker<IHelloService>> invokers = new ArrayList<Invoker<IHelloService>>();
	
	@Before
	public void beforeMethod(){
		invokers.clear();
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
	
	@Test
	public void testMockInvokerInvoke_forcemock_defaultreturn(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName());
		url = url.addParameter(Constants.MOCK_KEY, "force" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
	    URL mockUrl = URL.valueOf("mock://localhost/"+IHelloService.class.getName()
				+"?getSomething.mock=return aa&getSomething3xx.mock=return xx&sayHello.mock=return ")
				.addParameters(url.getParameters());
		
		Protocol protocol = new MockProtocol();
		Invoker<IHelloService> mInvoker1 = protocol.refer(IHelloService.class, mockUrl);
		invokers.add(mInvoker1);
	    
	    RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("sayHello");
	    Result ret = cluster.invoke(invocation);
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
	public void testMockInvokerFromOverride_Invoke_check_String(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getSomething.mock","force:return 1688")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getSomething");
        Result ret = cluster.invoke(invocation);
        Assert.assertTrue("result type must be String but was : " + ret.getValue().getClass(), ret.getValue() instanceof String);
        Assert.assertEquals("1688", (String)ret.getValue());
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
        Assert.assertTrue("result type must be integer but was : " + ret.getValue().getClass(), ret.getValue() instanceof Integer);
        Assert.assertEquals(new Integer(1688), (Integer)ret.getValue());
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
        Assert.assertTrue("result type must be Boolean but was : " + ret.getValue().getClass(), ret.getValue() instanceof Boolean);
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
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMockInvokerFromOverride_Invoke_check_ListString_empty(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getListString.mock","force:return empty")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getListString");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals(0, ((List<String>)ret.getValue()).size());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMockInvokerFromOverride_Invoke_check_ListString(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getListString.mock","force:return [\"hi\",\"hi2\"]")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getListString");
        Result ret = cluster.invoke(invocation);
        List<String> rl = (List<String>)ret.getValue();
        Assert.assertEquals(2, rl.size());
        Assert.assertEquals("hi", rl.get(0));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMockInvokerFromOverride_Invoke_check_ListPojo_empty(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getUsers.mock","force:return empty")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getUsers");
        Result ret = cluster.invoke(invocation);
        Assert.assertEquals(0, ((List<User>)ret.getValue()).size());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMockInvokerFromOverride_Invoke_check_ListPojo(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getUsers.mock","force:return [{id:1, name:\"hi1\"}, {id:2, name:\"hi2\"}]")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getUsers");
        Result ret = cluster.invoke(invocation);
        List<User> rl = (List<User>)ret.getValue();
        System.out.println(rl);
        Assert.assertEquals(2, rl.size());
        Assert.assertEquals("hi1", ((User)rl.get(0)).getName());
	}
	
	@Test
	public void testMockInvokerFromOverride_Invoke_check_ListPojo_error(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getUsers.mock","force:return [{id:x, name:\"hi1\"}]")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getUsers");
		try{
			cluster.invoke(invocation);
		}catch (RpcException e) {
		}
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
			Assert.assertFalse("not custem exception", e.isBiz());
		}
	}
	
	@Test
	public void testMockInvokerFromOverride_Invoke_force_throwCustemException() throws Throwable{
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getBoolean2.mock","force:throw com.alibaba.dubbo.rpc.cluster.support.wrapper.MyMockException")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getBoolean2");
		try {
			cluster.invoke(invocation).recreate();
			Assert.fail();
		} catch (MyMockException e) {
			
		}
	}
	
	@Test
	public void testMockInvokerFromOverride_Invoke_force_throwCustemExceptionNotFound(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("getBoolean2.mock","force:throw java.lang.RuntimeException2")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getBoolean2");
		try {
			cluster.invoke(invocation);
			Assert.fail();
		} catch (Exception e) {
			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
		}
	}
	
	@Test
	public void testMockInvokerFromOverride_Invoke_mock_false(){
		URL url = URL.valueOf("remote://1.2.3.4/"+IHelloService.class.getName())
				.addParameter("mock","false")
				.addParameter("invoke_return_error", "true" );
		Invoker<IHelloService> cluster = getClusterInvoker(url);        
		//方法配置了mock
        RpcInvocation invocation = new RpcInvocation();
		invocation.setMethodName("getBoolean2");
		try {
			cluster.invoke(invocation);
			Assert.fail();
		} catch (RpcException e) {
			Assert.assertTrue(e.isTimeout());
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
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
            		throw new RpcException(RpcException.TIMEOUT_EXCEPTION, "test rpc exception");
            	} else {
            		return ((Invoker<?>)invokers.get(0)).invoke(invocation);
            	}
            }
        };
        return new MockClusterInvoker<IHelloService>(dic, cluster);
	}
	
	public static interface IHelloService{
		String getSomething();
		String getSomething2();
		String getSomething3();
		int getInt1();
		boolean getBoolean1();
		Boolean getBoolean2();
		public List<String> getListString();
		public List<User> getUsers();
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
		public List<String> getListString() {
			return Arrays.asList(new String[]{"Tom","Jerry"});
		}
		public List<User> getUsers() {
			return Arrays.asList(new User[]{new User(1,"Tom"),new User(2,"Jerry")});
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
		public List<String> getListString() {
			return Arrays.asList(new String[]{"Tommock","Jerrymock"});
		}
		public List<User> getUsers() {
			return Arrays.asList(new User[]{new User(1,"Tommock"),new User(2,"Jerrymock")});
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
	
	public static class User{
		private int id;
		private String name;
		
		public User() {
		}
		public User(int id, String name) {
			super();
			this.id = id;
			this.name = name;
		}
		public int getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public void setId(int id) {
			this.id = id;
		}
		public void setName(String name) {
			this.name = name;
		}
		
	}
}
