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
package com.alibaba.dubbo.rpc.protococol.injvm;


import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import com.alibaba.dubbo.common.ExtensionLoader;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.service.EchoService;

/**
 * <code>ProxiesTest</code>
 */

public class ProtocolsTest
{
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

	@Test
	public void testLocalProtocol() throws Exception
	{
		DemoService service = new DemoServiceImpl();
		protocol.export(proxy.getInvoker(service, DemoService.class, URL.valueOf("injvm://127.0.0.1/TestService")));
		service = proxy.getProxy(protocol.refer(DemoService.class, URL.valueOf("injvm://127.0.0.1/TestService")));
		assertEquals(service.getSize(new String[]{"", "", ""}), 3);
		service.invoke("injvm://127.0.0.1/TestService", "invoke");
	}

}