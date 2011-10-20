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
package com.alibaba.dubbo.rpc.support;

import java.net.InetSocketAddress;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;

/**
 * AbstractExporter.
 * 
 * @author qianlei
 * @author william.liangf
 */
public abstract class AbstractExporter<T> implements Exporter<T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Invoker<T> invoker;

    private volatile boolean unexported = false;

	public AbstractExporter(Invoker<T> invoker) {
        if(invoker == null)
            throw new IllegalStateException("service invoker == null");
        if(invoker.getInterface() == null)
            throw new IllegalStateException("service type == null");
        if(invoker.getUrl() == null)
            throw new IllegalStateException("service url == null");
		this.invoker = invoker;
	}
	
	public Invoker<T> getInvoker() {
	    return invoker;
	}

	public void unexport() {
	    if (unexported)
	        throw new IllegalStateException("The exporter " + this + " unexported!");
	    unexported = true;
	    getInvoker().destroy();
	}

    public String toString() {
        return getInvoker().toString();
    }

    /**
     * invoke.
     * 
     * <code>
     *     Context.getContext().setRemoteAddress(remoteAddress);
     *     getInvoker().invoke(invocation);
     * </code>
     * 
     * @param invocation
     * @param remoteAddress
     * @return
     * @throws RpcException
     */
    public Result invoke(Invocation invocation, InetSocketAddress remoteAddress) throws RpcException {
        RpcContext.getContext().setRemoteAddress(remoteAddress);
        return getInvoker().invoke(invocation);
    }
    
    /**
     * invoke.
     * 
     * <code>
     *     Context.getContext().setRemoteAddress(remoteHost, remotePort);
     *     getInvoker().invoke(invocation);
     * </code>
     * 
     * @param invocation
     * @param remoteHost
     * @param remotePort
     * @return
     * @throws RpcException
     */
    public Result invoke(Invocation invocation, String remoteHost, int remotePort) throws RpcException {
        if (remoteHost != null && remoteHost.length() > 0) {
            if (remotePort < 0) {
                remotePort = 0;
            }
            RpcContext.getContext().setRemoteAddress(remoteHost, remotePort);
        }
        return getInvoker().invoke(invocation);
    }
    
}