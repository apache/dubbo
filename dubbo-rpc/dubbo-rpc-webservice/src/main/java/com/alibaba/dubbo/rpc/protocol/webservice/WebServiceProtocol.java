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
package com.alibaba.dubbo.rpc.protocol.webservice;

import java.io.IOException;
import java.net.SocketTimeoutException;

import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;

/**
 * WebServiceProtocol.
 * 
 * @author netcomm
 */
public class WebServiceProtocol extends AbstractProxyProtocol {
    
    public static final int DEFAULT_PORT = 80;

    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    protected <T> Runnable doExport(T impl, Class<T> type, URL url) throws RpcException {
        final JaxWsServerFactoryBean jaxWsServerFactoryBean = new JaxWsServerFactoryBean();
        jaxWsServerFactoryBean.setServiceClass(type);
        jaxWsServerFactoryBean.setAddress(url.setProtocol("http").toString());
        jaxWsServerFactoryBean.setServiceBean(impl);
        jaxWsServerFactoryBean.create();
        return new Runnable() {
            public void run() {
                jaxWsServerFactoryBean.destroy();
            }
        };
    }

    @SuppressWarnings("unchecked")
    protected <T> T doRefer(final Class<T> serviceType, final URL url) throws RpcException {
    	JaxWsProxyFactoryBean jaxWsProxyFactoryBean = new JaxWsProxyFactoryBean();
    	jaxWsProxyFactoryBean.getInInterceptors().add(new LoggingInInterceptor());
    	jaxWsProxyFactoryBean.getOutInterceptors().add(new LoggingOutInterceptor());
    	jaxWsProxyFactoryBean.setServiceClass(serviceType);
    	jaxWsProxyFactoryBean.setAddress(url.setProtocol("http").toString());
    	return (T) jaxWsProxyFactoryBean.create();
    }

    protected int getErrorCode(Throwable e) {
        if (e instanceof SocketTimeoutException) {
            return RpcException.TIMEOUT_EXCEPTION;
        } else if (e instanceof IOException) {
            return RpcException.NETWORK_EXCEPTION;
        }
        return super.getErrorCode(e);
    }

}