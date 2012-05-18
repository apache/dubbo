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
package com.alibaba.dubbo.rpc.protocol.rmi;


import static junit.framework.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.service.EchoService;

public class RmiProtocolTest
{
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    
    
    public static interface NonStdRmiInterface {
        void bark();
    }
    /*
    @Test
    public void test_getRemoteClass() throws Exception {
        Class<NonStdRmiInterface> clazz = RmiProtocol.getRemoteClass(NonStdRmiInterface.class);
        assertEquals(clazz, RmiProtocol.getRemoteClass(NonStdRmiInterface.class));
    }
    */
    @Test
    public void testRmiProtocolTimeout() throws Exception
    {
        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "1000");
        DemoService service = new DemoServiceImpl();
        Exporter<?> rpcExporter = protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("rmi://127.0.0.1:9001/TestService")));
        service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("rmi://127.0.0.1:9001/TestService")));
        try {
            try {
                service.throwTimeout();
            } catch (RpcException e) {
                assertEquals(true, e.isTimeout());
                assertEquals(true, e.getMessage().contains("Read timed out"));
            }
        } finally {
            rpcExporter.unexport();
        }
    }
    
	@Test
	public void testRmiProtocol() throws Exception
	{
	    {
    		DemoService service = new DemoServiceImpl();
    		Exporter<?> rpcExporter = protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("rmi://127.0.0.1:9001/TestService")));
    		
    		service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("rmi://127.0.0.1:9001/TestService")));
    		assertEquals(service.getSize(null), -1);
    		assertEquals(service.getSize(new String[]{"", "", ""}), 3);
    		Object result = service.invoke("rmi://127.0.0.1:9001/TestService", "invoke");
    		assertEquals("rmi://127.0.0.1:9001/TestService:invoke", result);
    		
    		rpcExporter.unexport();
	    }

	    {
    		RemoteService remoteService = new RemoteServiceImpl();
    		Exporter<?> rpcExporter = protocol.export(proxy.getInvoker(remoteService, RemoteService.class, URL.valueOf("rmi://127.0.0.1:9002/remoteService")));
    		
    		remoteService = proxy.getProxy(protocol.refer(RemoteService.class, URL.valueOf("rmi://127.0.0.1:9002/remoteService")));
    		remoteService.getThreadName();
    		for(int i=0;i<100;i++) {
                String say = remoteService.sayHello("abcd");
                assertEquals("hello abcd@" + RemoteServiceImpl.class.getName(), say);
            }
    		rpcExporter.unexport();
	    }
	}
	
	// FIXME RMI协议目前的实现不支持转型成 EchoService
	@Ignore
	@Test
	public void testRmiProtocol_echoService() throws Exception
    {
	    DemoService service = new DemoServiceImpl();
	    Exporter<?> rpcExporter = protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("rmi://127.0.0.1:9002/TestService")));
        
	    // cast to EchoService
        EchoService echo = proxy.getProxy(protocol.refer(EchoService.class, URL.valueOf("rmi://127.0.0.1:9002/TestService")));
        assertEquals(echo.$echo("test"), "test");
        assertEquals(echo.$echo("abcdefg"), "abcdefg");
        assertEquals(echo.$echo(1234), 1234);
        
        rpcExporter.unexport();
        
        RemoteService remoteService = new RemoteServiceImpl();
        rpcExporter = protocol.export(proxy.getInvoker(remoteService, RemoteService.class, URL.valueOf("rmi://127.0.0.1:9002/remoteService")));
        
        // cast to EchoService
        echo = proxy.getProxy(protocol.refer(EchoService.class, URL.valueOf("rmi://127.0.0.1:9002/remoteService")));
        assertEquals(echo.$echo("test"), "test");
        assertEquals(echo.$echo("abcdefg"), "abcdefg");
        assertEquals(echo.$echo(1234), 1234);
        
        rpcExporter.unexport();
    }

	/*@Test
	public void testRpcInvokerGroup() throws Exception
	{
		DemoService service = new DemoServiceImpl();
		RpcUtils.export("demo://127.0.0.1:9030/com.alibaba.dubbo.rpc.TestService",DemoService.class,service);
		RpcUtils.export("dubbo://127.0.0.1:9031/TestService",DemoService.class,service);
		RpcUtils.export("rmi://127.0.0.1:9032/com.alibaba.dubbo.rpc.TestService",DemoService.class,service);
		RpcUtils.export("rmi://127.0.0.1:9033/com.alibaba.dubbo.rpc.TestService",DemoService.class,service);

		service = RpcUtils.createProxy(DemoService.class,
				new String[]{
					"demo://127.0.0.1:9030/com.alibaba.dubbo.rpc.TestService?weight=20",
					"dubbo://127.0.0.1:9031/TestService?weight=20",
					"rmi://127.0.0.1:9032/com.alibaba.dubbo.rpc.TestService",
				});
		assertEquals(service.getSize(null), -1);
		assertEquals(service.getSize(new String[]{"","",""}), 3);

		// cast to EchoService
		EchoService echo = RpcUtils.createProxy(EchoService.class,
				new String[]{
			"demo://127.0.0.1:9030/com.alibaba.dubbo.rpc.TestService?weight=20",
			"dubbo://127.0.0.1:9031/TestService?weight=20",
			"rmi://127.0.0.1:9032/com.alibaba.dubbo.rpc.TestService",
		});
		assertEquals(echo.$echo("test"), "test");
		assertEquals(echo.$echo("abcdefg"), "abcdefg");
		assertEquals(echo.$echo(1234), 1234);
	}*/

	/*public void testForkInvoke() throws Exception
	{
		DemoService service = new DemoServiceImpl();
		protocol.export(proxy.createInvoker("dubbo://127.0.0.1:9040/TestService", DemoService.class, service);
		protocol.export(proxy.createInvoker("dubbo://127.0.0.1:9041/TestService", DemoService.class, service);
		protocol.export(proxy.createInvoker("rmi://127.0.0.1:9042/com.alibaba.dubbo.rpc.TestService", DemoService.class, service);
		protocol.export(proxy.createInvoker("rmi://127.0.0.1:9043/com.alibaba.dubbo.rpc.TestService", DemoService.class, service);

		RpcInvokerGroup group = Proxies.createInvoker(DemoService.class, new String[]{
			"dubbo://127.0.0.1:9040/TestService",
			"dubbo://127.0.0.1:9041/TestService",
			"rmi://127.0.0.1:9042/com.alibaba.dubbo.rpc.TestService",
			"rmi://127.0.0.1:9043/com.alibaba.dubbo.rpc.TestService",
		});
		group.getMethodSettings("echo").setFork(true);
		group.getMethodSettings("echo").setForkInvokeCallback(new ForkInvokeCallback(){
			public Object merge(RpcInvocation invocation, RpcResult[] results) throws Throwable
			{
				System.out.println("merge result begin:");
				for( RpcResult result : results )
				{
					if( result.hasException() )
						System.out.println("exception:"+result.getException().getMessage());
					else
						System.out.println("result:"+result.getResult());
				}
				System.out.println("merge result end:");
				return "aaaa";
			}
		});

		service = proxy.createProxy(protocol.refer(DemoService.class, group);
		service.echo("test");

		// cast to EchoService
		EchoService echo = proxy.createProxy(protocol.refer(EchoService.class, group);
		assertEquals(echo.$echo("test"), "test");
		assertEquals(echo.$echo("abcdefg"), "abcdefg");
		assertEquals(echo.$echo(1234), 1234);
	}*/

}