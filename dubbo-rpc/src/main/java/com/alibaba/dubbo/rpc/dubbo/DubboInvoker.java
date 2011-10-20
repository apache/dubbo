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
package com.alibaba.dubbo.rpc.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.AtomicPositiveInteger;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.TimeoutException;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ResponseFuture;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcConstants;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.support.AbstractInvoker;
import com.alibaba.dubbo.rpc.support.FutureAdapter;

/**
 * DubboInvoker
 * 
 * @author william.liangf
 * @author chao.liuc
 */
public class DubboInvoker<T> extends AbstractInvoker<T> {

    private final ExchangeClient[]      clients;

    private final AtomicPositiveInteger index = new AtomicPositiveInteger();

    private final String                version;
    
    public DubboInvoker(Class<T> serviceType, URL url, ExchangeClient[] clients){
        super(serviceType, url, new String[] {Constants.GROUP_KEY, Constants.TOKEN_KEY, Constants.TIMEOUT_KEY});
        this.clients = clients;
        // get version.
        this.version = url.getParameter(Constants.VERSION_KEY, "0.0.0");
    }

    @Override
    protected Object doInvoke(final Invocation invocation) throws Throwable {
        RpcInvocation inv = null;
        final String methodName  ;
        if(Constants.$INVOKE.equals(invocation.getMethodName()) && invocation.getArguments() !=null && invocation.getArguments().length >0 && invocation.getArguments()[0] != null){
            inv = (RpcInvocation) invocation;
            //the frist argument must be real method name;
            methodName = invocation.getArguments()[0].toString();
        }else {
            inv = new RpcInvocation(invocation.getMethodName(), invocation.getParameterTypes(),
                    invocation.getArguments(), invocation.getAttachments());
            methodName = invocation.getMethodName();
        }
        inv.setAttachment(Constants.PATH_KEY, getUrl().getPath());
        inv.setAttachment(Constants.VERSION_KEY, version);
        
        ExchangeClient currentClient;
        if (clients.length == 1) {
            currentClient = clients[0];
        } else {
            currentClient = clients[index.getAndIncrement() % clients.length];
        }
        Result result = null ;
        try {
            // 不可靠异步
            boolean isAsync = getUrl().getMethodBooleanParameter(methodName, Constants.ASYNC_KEY);
            int timeout = getUrl().getMethodIntParameter(methodName, Constants.TIMEOUT_KEY,Constants.DEFAULT_TIMEOUT);
            if (isAsync) { 
                boolean isReturn = getUrl().getMethodBooleanParameter(methodName, RpcConstants.RETURN_KEY, true);
                if (isReturn) {
                    ResponseFuture future = currentClient.request(inv, timeout) ;
                    RpcContext.getContext().setFuture(new FutureAdapter<Object>(future));
                } else {
                    boolean isSent = getUrl().getMethodBooleanParameter(methodName, Constants.SENT_KEY);
                    currentClient.send(inv, isSent);
                    RpcContext.getContext().setFuture(null);
                }
                return null;
            }
            RpcContext.getContext().setFuture(null);
            result = (Result) currentClient.request(inv, timeout).get();
        } catch (RpcException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, e.getMessage(), e);
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, e.getMessage(), e);
        } catch (Throwable e) { // here is non-biz exception, wrap it.
            throw new RpcException(e.getMessage(), e);
        }
        //attention: recreate can not in try-catch block. 
        return result == null ? null: result.recreate();
    }
    
    @Override
    public boolean isAvailable() {
        if (!super.isAvailable())
            return false;
        if (clients.length ==1){
            return clients[0].isConnected();
        } else {
            for (ExchangeClient client : clients){
                if (client.isConnected()){
                    return true;
                }
            }
        }
        return false;
    }

    public void destroy() {
        super.destroy();
        for (ExchangeClient client : clients) {
            try {
                client.close();
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
    }
    
}