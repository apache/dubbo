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
package com.alibaba.dubbo.rpc.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.Exchangers;
import com.alibaba.dubbo.remoting.exchange.support.Replier;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.support.AbstractExporter;
import com.alibaba.dubbo.rpc.support.AbstractInvoker;
import com.alibaba.dubbo.rpc.support.AbstractProtocol;

/**
 * TestProtocolSupport.
 * 
 * @author qian.lei
 */
@Extension("demo")
public class DemoRpcProtocol extends AbstractProtocol {

	private final Map<String, ExchangeChannel> mClientMap = new HashMap<String, ExchangeChannel>(); // <remote ip:remote port,Client>

	private final Map<Integer, ExchangeServer> mServerMap = new HashMap<Integer, ExchangeServer>(); // <port,Server>

	private final Map<String, InternalExporter<?>> mServiceMap = new HashMap<String, InternalExporter<?>>(); // <service name@port,service instance>

	private final Replier<DemoRequest> mHandler = new InternalHandler();

    public int getDefaultPort() {
        return 123456;
    }
    
	public <T> Invoker<T> refer(Class<T> serviceType, URL url) throws RpcException
	{
	    ExchangeChannel client = mClientMap.get(url.getHost() + ':' + url.getPort());
		if( client == null )
		{
		    // create client.
            try {
                client = Exchangers.connect(url);
            } catch (RemotingException e) {
                throw new RpcException(e.getMessage(), e);
            }
			mClientMap.put(url.getHost() + ':' + url.getPort(),client);
		}
		final ExchangeChannel fc = client;
		return new AbstractInvoker<T>(serviceType, url){
			public Object doInvoke(Invocation invocation) throws Throwable
			{
				DemoRequest req = new DemoRequest(getUrl().getPath(), invocation.getMethodName(), invocation.getParameterTypes(), invocation.getArguments());
				int timeout = getUrl().getMethodIntParameter(invocation.getMethodName(), "timeout");
				if( timeout > 0 )
					return fc.request(req, timeout).get();
				return fc.request(req).get();
			}
		};
	}

	public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        URL url = invoker.getUrl();
		if( !mServerMap.containsKey(url.getPort()) )
		{
		    ExchangeServer server;
            try {
                server = Exchangers.bind(url, mHandler);
            } catch (RemotingException e) {
                throw new RpcException(e.getMessage(), e);
            }
			mServerMap.put(url.getPort(),server);
		}
		InternalExporter<T> exporter = new InternalExporter<T>(invoker);
		mServiceMap.put(url.getPath()+'@'+url.getPort(), exporter);
		return exporter;
	}

	public void destroy()
	{
		for (String key : new ArrayList<String>(mServiceMap.keySet())) {
			InternalExporter<?> exporter = mServiceMap.remove(key);
			if (exporter != null) {
				try {
					exporter.unexport();
				} catch (Throwable t) {
					logger.warn(t.getMessage(), t);
				}
			}
		}
		for (Integer key : new ArrayList<Integer>(mServerMap.keySet())) {
			ExchangeServer server = mServerMap.remove(key);
			if (server != null) {
				try {
					server.close();
				} catch (Throwable t) {
					logger.warn(t.getMessage(), t);
				}
			}
		}
		for (String key : new ArrayList<String>(mClientMap.keySet())) {
			ExchangeChannel client = mClientMap.remove(key);
			if (client != null) {
				try {
					client.close();
				} catch (Throwable t) {
					logger.warn(t.getMessage(), t);
				}
			}
		}
	}

	private static class InternalExporter<T> extends AbstractExporter<T>
	{
		public InternalExporter(Invoker<T> invoker)
		{
			super(invoker);
		}
	}

	private class InternalHandler implements Replier<DemoRequest> {
		public Object reply(ExchangeChannel channel, DemoRequest msg) throws RemotingException {
			// find service instance.
			int port = channel.getLocalAddress().getPort();
			String serviceName = msg.getServiceName();
			InternalExporter<?> exporter = mServiceMap.get(serviceName+'@'+port);
			if( exporter == null )
				throw new RemotingException(channel, "Service " + serviceName + " not found.");
			try {
                return exporter.invoke(new RpcInvocation(msg.getMethodName(), msg.getParameterTypes(), msg.getArguments()), channel.getRemoteAddress()).recreate();
            } catch (RpcException e) {
                throw new RemotingException(channel, e.getMessage(), e);
            } catch (Throwable e) {
                throw new RemotingException(channel, e.getMessage(), e);
            }
		}
	}
}