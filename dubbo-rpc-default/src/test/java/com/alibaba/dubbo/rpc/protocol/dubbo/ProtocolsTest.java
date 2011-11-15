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
package com.alibaba.dubbo.rpc.protocol.dubbo;


import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoServiceImpl;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.Type;
import com.alibaba.dubbo.rpc.service.EchoService;

/**
 * <code>ProxiesTest</code>
 */

public class ProtocolsTest
{
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    
	@Test
	public void testDemoProtocol() throws Exception
	{
		DemoService service = new DemoServiceImpl();
		protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("dubbo://127.0.0.1:9020/TestService?codec=exchange")));
		service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("dubbo://127.0.0.1:9020/TestService?codec=exchange")));
		assertEquals(service.getSize(new String[]{"", "", ""}), 3);
	}

	@Test
	public void testDubboProtocol() throws Exception
	{
		DemoService service = new DemoServiceImpl();
		protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("dubbo://127.0.0.1:9010/TestService?service.filter=echo")));
		service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("dubbo://127.0.0.1:9010/TestService")));
		assertEquals(service.enumlength(new Type[]{}), Type.Lower);
		assertEquals(service.getSize(null), -1);
		assertEquals(service.getSize(new String[]{"", "", ""}), 3);
		service.invoke("dubbo://127.0.0.1:9010/TestService", "invoke");

		service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("dubbo://127.0.0.1:9010/TestService?client=netty")));
		// test netty client
		StringBuffer buf = new StringBuffer();
		for(int i=0;i<1024*32+32;i++)
			buf.append('A');
		System.out.println(service.stringLength(buf.toString()));

		// cast to EchoService
		EchoService echo = proxy.getProxy(protocol.refer(EchoService.class, URL.valueOf("dubbo://127.0.0.1:9010/TestService?client=netty")));
		assertEquals(echo.$echo(buf.toString()), buf.toString());
		assertEquals(echo.$echo("test"), "test");
		assertEquals(echo.$echo("abcdefg"), "abcdefg");
		assertEquals(echo.$echo(1234), 1234);
	}

	@Test
	public void testPerm() throws Exception
	{
		DemoService service = new DemoServiceImpl();
		protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("dubbo://127.0.0.1:9050/TestService?codec=exchange")));
		service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("dubbo://127.0.0.1:9050/TestService?codec=exchange")));
		long start = System.currentTimeMillis();
		for(int i=0;i<1000;i++)
			service.getSize(new String[]{"", "", ""});
		System.out.println("take:"+(System.currentTimeMillis()-start));
	}
}