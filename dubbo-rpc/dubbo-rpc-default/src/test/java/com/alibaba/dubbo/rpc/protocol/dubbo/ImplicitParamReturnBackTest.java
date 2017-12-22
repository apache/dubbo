/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoService;
import com.alibaba.dubbo.rpc.protocol.dubbo.support.DemoServiceImpl;

import junit.framework.TestCase;

/**
 * test for #889、#895
 * @author hsrong
 *
 */
public class ImplicitParamReturnBackTest extends TestCase {
	
	public static final String SOME_ATTACHMENT_KEY = "SOME_ATTACHMENT_KEY";
	public static final String SOME_ATTACHMENT_VALUE = "the value come from ProviderFilter";
	
    private Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
    private ProxyFactory proxy = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
    {
    	ExtensionLoader.getExtensionLoader(Filter.class).addExtension("ConsumerFilter", ConsumerFilter.class);
    	ExtensionLoader.getExtensionLoader(Filter.class).addExtension("ProviderFilter", ProviderFilter.class);
    }

    public void testImplicitParamReturnBack() throws Exception {
        DemoService service = new DemoServiceImpl();
        URL providerURL = URL.valueOf("dubbo://127.0.0.1:9010/com.alibaba.dubbo.rpc.DemoService?service.filter=ProviderFilter");
        protocol.export(proxy.getInvoker(service, DemoService.class, providerURL));
        URL consumerURL = URL.valueOf("dubbo://127.0.0.1:9010/com.alibaba.dubbo.rpc.DemoService?service.filter=ConsumerFilter");
        service = proxy.getProxy(protocol.refer(DemoService.class, consumerURL));
        service.echo("123");
    }
    
    public static class ConsumerFilter implements Filter {

    	@Override
    	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
    		Result result = invoker.invoke(invocation);
    		System.out.println("get Attachments in ConsumerFilter: " + SOME_ATTACHMENT_KEY + "=" + result.getAttachment(SOME_ATTACHMENT_KEY));
    		assertEquals(SOME_ATTACHMENT_VALUE, result.getAttachment(SOME_ATTACHMENT_KEY));
    		return result;
    	}

    }
    
    public static class ProviderFilter implements Filter {

    	@Override
    	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
    		Result result = invoker.invoke(invocation);
    		result.getAttachments().put(SOME_ATTACHMENT_KEY, SOME_ATTACHMENT_VALUE);
    		System.out.println("set Attachments in ProviderFilter: " + SOME_ATTACHMENT_KEY + "=" + result.getAttachment(SOME_ATTACHMENT_KEY));
    		return result;
    	}

    }

}