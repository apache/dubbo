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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.StubNotFoundException;
import java.rmi.UnknownHostException;
import java.rmi.registry.Registry;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.NO_RESPONSE;
import org.omg.CORBA.SystemException;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;

/**
 * RmiInvoker.
 * 
 * @author qian.lei
 */
public class RmiInvoker<T> extends AbstractInvoker<T> {
    
    private Registry registry;
    
    private RmiProxyFactory rmiProxyFactory;

    private Invoker<T> invoker;
    
    private boolean reconnect;

    public RmiInvoker(Registry registry, RmiProxyFactory rmiProxyFactory, Invoker<T> invoker) {
        super(invoker.getInterface(), invoker.getUrl());
        this.registry = registry;
        this.rmiProxyFactory = rmiProxyFactory;
        this.invoker = invoker;
        this.reconnect = invoker.getUrl().getParameter(Constants.RECONNECT_KEY, true);
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws RpcException {
        Result result = null;
        try {
            result = invoker.invoke(invocation);
            
            // 对Rmi的Connection问题进行重试
            Throwable e = result.getException();
            if (e != null && isConnectFailure(e) && reconnect) {
                invoker = rmiProxyFactory.getInvoker(registry.lookup(invoker.getUrl().getPath()), invoker.getInterface(), invoker.getUrl());
                result = invoker.invoke(invocation);
            }
        } catch (RpcException e) {
            throw setRpcExceptionCode(e.getCause(), e);
        } catch (Throwable e) {
            throw setRpcExceptionCode(e, new RpcException(e.getMessage(), e));
        }

        Throwable e = result.getException();
        if (e != null && e instanceof RemoteException) {
            throw setRpcExceptionCode(e, new RpcException("Failed to invoke remote service: " + getInterface() + ", method: "
                    + invocation.getMethodName() + ", url: " + invoker.getUrl() + ", cause: " + e.getMessage(), e));
        }
        return result;
    }
    
    private RpcException setRpcExceptionCode(Throwable e, RpcException re) {
        if (e != null && e.getCause() != null) {
            Class<?> cls = e.getCause().getClass();
            // 是根据测试Case发现的问题，对RpcException.setCode进行设置
            if (SocketTimeoutException.class.equals(cls)) {
                re.setCode(RpcException.TIMEOUT_EXCEPTION);
            } else if (IOException.class.isAssignableFrom(cls)) {
                re.setCode(RpcException.NETWORK_EXCEPTION);
            } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                re.setCode(RpcException.SERIALIZATION_EXCEPTION);
            }
        }
        return re;
    }

    private static final String ORACLE_CONNECTION_EXCEPTION = "com.evermind.server.rmi.RMIConnectionException";

    private static boolean isConnectFailure(Throwable ex) {
        return (ex instanceof ConnectException || ex instanceof ConnectIOException ||
                ex instanceof UnknownHostException || ex instanceof NoSuchObjectException ||
                ex instanceof StubNotFoundException || isCorbaConnectFailure(ex.getCause()) ||
                ORACLE_CONNECTION_EXCEPTION.equals(ex.getClass().getName()));
    }

    private static boolean isCorbaConnectFailure(Throwable ex) {
        return ((ex instanceof COMM_FAILURE || ex instanceof NO_RESPONSE) &&
                ((SystemException) ex).completed == CompletionStatus.COMPLETED_NO);
    }
    
}