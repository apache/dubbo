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
package com.alibaba.dubbo.rpc.protocol.jms;

import java.io.Serializable;
import java.net.SocketTimeoutException;

import javax.jms.*;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;
import com.alibaba.dubbo.rpc.protocol.jms.implementor.JmsImplementor;

/**
 * @author Kimmking
 */
public class JmsRpcInvoker<T> extends AbstractInvoker<T> {

    protected static final String JMS_EXCEPTION_PREFIX = JMSException.class.getPackage().getName() + ".";

    public static final int RpcException_JMS_EXCEPTION = 10;

    private final JmsImplementor jmsImplementor = ExtensionLoader.getExtensionLoader(JmsImplementor.class).getAdaptiveExtension();


    final QueueConnectionFactory factory;
    QueueConnection connection;
    QueueSession session;
    Queue queue;

    protected Invoker<T>   invoker;
    long timeout;

    public JmsRpcInvoker(Class<T> serviceType, URL url, ProxyFactory proxyFactory) {
        super(serviceType, url);

        timeout = url.getParameter("timeout", 100L);// 60ms is default
        factory = jmsImplementor.getQueueConnectionFactory(url);
        try {
            connection = factory.createQueueConnection();
            connection.start();
            session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            queue = session.createQueue("DUBBO");
        } catch (JMSException e) {
            throw new RpcException("Fail to create JMS Connector to " + url + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws Throwable {
        try {
            Message message = session.createObjectMessage((Serializable)invocation);
            QueueSender sender = session.createSender(queue);
            sender.send(message);

            return new RpcResult();
        } catch (RpcException e) {
            throw setRpcExceptionCode(e.getCause(), e);
        } catch (Throwable e) {
            return new RpcResult(e);
        }
    }

    private RpcException setRpcExceptionCode(Throwable e, RpcException re) {
        if (e != null) {
            if (e.getClass().getName().startsWith("java.net")) {
                re.setCode(RpcException.NETWORK_EXCEPTION);
                if (e.getCause() != null) {
                    Class<?> cls = e.getCause().getClass();
                    if (SocketTimeoutException.class.equals(cls)) {
                        re.setCode(RpcException.TIMEOUT_EXCEPTION);
                    }
                }
            } else if (e instanceof JMSException) {
                re.setCode(RpcException_JMS_EXCEPTION);
            }
        }
        return re;
    }

    @Override
    public void destroy() {
        super.destroy();

        try {
            session.close();
            connection.close();
        } catch (JMSException e) {

        }
    }

}
