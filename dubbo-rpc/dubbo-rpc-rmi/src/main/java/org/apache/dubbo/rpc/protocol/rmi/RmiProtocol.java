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
package org.apache.dubbo.rpc.protocol.rmi;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.AbstractProxyProtocol;
import org.apache.dubbo.rpc.service.GenericService;
import org.apache.dubbo.rpc.support.ProtocolUtils;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.remoting.rmi.RmiServiceExporter;
import org.springframework.remoting.support.RemoteInvocation;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.rmi.RemoteException;

import static org.apache.dubbo.common.Version.isRelease263OrHigher;
import static org.apache.dubbo.common.Version.isRelease270OrHigher;

/**
 * RmiProtocol.
 */
public class RmiProtocol extends AbstractProxyProtocol {

    public static final int DEFAULT_PORT = 1099;

    public RmiProtocol() {
        super(RemoteAccessException.class, RemoteException.class);
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(final T impl, Class<T> type, URL url) throws RpcException {
        RmiServiceExporter rmiServiceExporter = createExporter(impl, type, url, false);
        RmiServiceExporter genericServiceExporter = createExporter(impl, GenericService.class, url, true);
        return new Runnable() {
            @Override
            public void run() {
                try {
                    rmiServiceExporter.destroy();
                    genericServiceExporter.destroy();
                } catch (Throwable e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRefer(final Class<T> serviceType, final URL url) throws RpcException {
        final RmiProxyFactoryBean rmiProxyFactoryBean = new RmiProxyFactoryBean();
        final String generic = url.getParameter(Constants.GENERIC_KEY);
        final boolean isGeneric = ProtocolUtils.isGeneric(generic) || serviceType.equals(GenericService.class);
        /*
          RMI needs extra parameter since it uses customized remote invocation object

          The customized RemoteInvocation was firstly introduced in v2.6.3; The package was renamed to 'org.apache.*' since v2.7.0
          Considering the above two conditions, we need to check before sending customized RemoteInvocation:
          1. if the provider version is v2.7.0 or higher, send 'org.apache.dubbo.rpc.protocol.rmi.RmiRemoteInvocation'.
          2. if the provider version is v2.6.3 or higher, send 'com.alibaba.dubbo.rpc.protocol.rmi.RmiRemoteInvocation'.
          3. if the provider version is lower than v2.6.3, does not use customized RemoteInvocation.
         */
        if (isRelease270OrHigher(url.getParameter(Constants.RELEASE_KEY))) {
            rmiProxyFactoryBean.setRemoteInvocationFactory((methodInvocation) -> {
                RemoteInvocation invocation = new RmiRemoteInvocation(methodInvocation);
                if (invocation != null && isGeneric) {
                    invocation.addAttribute(Constants.GENERIC_KEY, generic);
                }
                return invocation;
            });
        } else if (isRelease263OrHigher(url.getParameter(Constants.DUBBO_VERSION_KEY))) {
            rmiProxyFactoryBean.setRemoteInvocationFactory((methodInvocation) -> {
                RemoteInvocation invocation = new com.alibaba.dubbo.rpc.protocol.rmi.RmiRemoteInvocation(methodInvocation);
                if (invocation != null && isGeneric) {
                    invocation.addAttribute(Constants.GENERIC_KEY, generic);
                }
                return invocation;
            });
        }
        String serviceUrl = url.toIdentityString();
        if (isGeneric) {
            serviceUrl = serviceUrl + "/" + Constants.GENERIC_KEY;
        }
        rmiProxyFactoryBean.setServiceUrl(serviceUrl);
        rmiProxyFactoryBean.setServiceInterface(serviceType);
        rmiProxyFactoryBean.setCacheStub(true);
        rmiProxyFactoryBean.setLookupStubOnStartup(true);
        rmiProxyFactoryBean.setRefreshStubOnConnectFailure(true);
        rmiProxyFactoryBean.afterPropertiesSet();
        return (T) rmiProxyFactoryBean.getObject();
    }

    @Override
    protected int getErrorCode(Throwable e) {
        if (e instanceof RemoteAccessException) {
            e = e.getCause();
        }
        if (e != null && e.getCause() != null) {
            Class<?> cls = e.getCause().getClass();
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

    private <T> RmiServiceExporter createExporter(T impl, Class<?> type, URL url, boolean isGeneric) {
        final RmiServiceExporter rmiServiceExporter = new RmiServiceExporter();
        rmiServiceExporter.setRegistryPort(url.getPort());
        if (isGeneric) {
            rmiServiceExporter.setServiceName(url.getPath() + "/" + Constants.GENERIC_KEY);
        } else {
            rmiServiceExporter.setServiceName(url.getPath());
        }
        rmiServiceExporter.setServiceInterface(type);
        rmiServiceExporter.setService(impl);
        try {
            rmiServiceExporter.afterPropertiesSet();
        } catch (RemoteException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return rmiServiceExporter;
    }

}
