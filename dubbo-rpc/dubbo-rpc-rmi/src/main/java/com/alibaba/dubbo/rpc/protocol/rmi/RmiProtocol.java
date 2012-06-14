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
import java.rmi.RemoteException;

import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.remoting.rmi.RmiServiceExporter;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;

/**
 * RmiProtocol.
 * 
 * @author qian.lei
 */
public class RmiProtocol extends AbstractProxyProtocol {

    public static final int              DEFAULT_PORT = 1099;

    public RmiProtocol() {
        super(RemoteAccessException.class, RemoteException.class);
    }

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    protected <T> Runnable doExport(final T impl, Class<T> type, URL url) throws RpcException {
        final RmiServiceExporter rmiServiceExporter = new RmiServiceExporter();
        rmiServiceExporter.setRegistryPort(url.getPort());
        rmiServiceExporter.setServiceName(url.getPath());
        rmiServiceExporter.setServiceInterface(type);
        rmiServiceExporter.setService(impl);
        try {
            rmiServiceExporter.afterPropertiesSet();
        } catch (RemoteException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return new Runnable() {
            public void run() {
                try {
                    rmiServiceExporter.destroy();
                } catch (Throwable e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected <T> T doRefer(final Class<T> serviceType, final URL url) throws RpcException {
        final RmiProxyFactoryBean rmiProxyFactoryBean = new RmiProxyFactoryBean();
        rmiProxyFactoryBean.setServiceUrl(url.toIdentityString());
        rmiProxyFactoryBean.setServiceInterface(serviceType);
        rmiProxyFactoryBean.setCacheStub(true);
        rmiProxyFactoryBean.setLookupStubOnStartup(true);
        rmiProxyFactoryBean.setRefreshStubOnConnectFailure(true);
        rmiProxyFactoryBean.afterPropertiesSet();
        return (T) rmiProxyFactoryBean.getObject();
    }

    protected int getErrorCode(Throwable e) {
        if (e instanceof RemoteAccessException) {
            e = e.getCause();
        }
        if (e != null && e.getCause() != null) {
            Class<?> cls = e.getCause().getClass();
            // 是根据测试Case发现的问题，对RpcException.setCode进行设置
            if (SocketTimeoutException.class.equals(cls)) {
                return RpcException.TIMEOUT_EXCEPTION;
            } else if (IOException.class.isAssignableFrom(cls)) {
                return RpcException.NETWORK_EXCEPTION;
            } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                return RpcException.SERIALIZATION_EXCEPTION;
            }
        }
        return super.getErrorCode(e);
    }

}