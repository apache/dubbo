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
package org.apache.dubbo.rpc.protocol.jms;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.protocol.jms.implementor.JmsImplementor;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.protocol.AbstractExporter;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;

/**
 * @author Kimmking
 * @author ding.lid
 */
public class JmsRpcExporter<T> extends AbstractExporter<T> {

    private static final Logger logger = LoggerFactory.getLogger(JmsRpcExporter.class);

    private final JmsImplementor jmsImplementor = ExtensionLoader.getExtensionLoader(JmsImplementor.class).getAdaptiveExtension();

    private final QueueConnectionFactory factory ;
    private final QueueConnection connection;
    private final QueueSession session;
    private final MessageConsumer consumer;

    public JmsRpcExporter(Invoker<T> invoker) {
        super(invoker);

        URL url = invoker.getUrl();
        try {
            factory = jmsImplementor.getQueueConnectionFactory(url);
            connection = factory.createQueueConnection();
            connection.start();

            session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            Queue queue = session.createQueue("DUBBO");

            consumer = session.createConsumer(queue);
            consumer.setMessageListener(new MyMessageListener());
        } catch (Exception e) {
            throw new RpcException("Fail to create JMS Connector to " + url + ", cause: " + e.getMessage(), e);
        }
    }

    @Override
    public void unexport() {
        super.unexport();

        try {
            if(consumer != null)
                consumer.close();
        } catch (Throwable t) {
            logger.warn("Error when close consumer of " + getInvoker().getUrl());
        }
    }

    class MyMessageListener<T> implements MessageListener {
        public MyMessageListener() {}

        public void onMessage(Message message) {
            try {
                ObjectMessage objectMessage = (ObjectMessage) message;
                Invocation invocation = (Invocation)objectMessage.getObject();

                Result result = getInvoker().invoke(invocation);
                if(result.hasException()) {
                    // TODO 业务异常。如何传递这个信息，处理的策略是？
                    // 业务异常打日志
                    logger.warn("Error when process message " + message + "Biz exception" + result.getException().getMessage(),
                            result.getException());
                }

                // Result中的返回值直接忽略！
                // TODO 业务返回值如何告知，打日志？
            } catch (Throwable t) {
                // TODO JMS异常。如何传递这个信息，处理的策略是？
                // JMS异常打日志
                logger.warn("Error when process message " + message + ", cause: " + t.getMessage(), t);
            }
        }
    }
}
