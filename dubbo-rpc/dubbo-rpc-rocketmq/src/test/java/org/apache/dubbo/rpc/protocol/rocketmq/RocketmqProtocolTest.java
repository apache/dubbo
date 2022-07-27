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

package org.apache.dubbo.rpc.protocol.rocketmq;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.rocketmq.client.exception.MQClientException;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RocketmqProtocolTest {

    private static RocketMQProtocolServer rocketMQProtocolServer = new RocketMQProtocolServer();
    private static RocketMQProtocol INSTANCE;
    private static String ROCKETMQ_URL_TEMPLATE = "rocketmq://124.223.198.143:9876";
    private static URL registryUrl;


    @BeforeEach
    public void setUp() {
        // init
        INSTANCE = RocketMQProtocol.getRocketMQProtocol();
    }

    @AfterEach
    public void tearDown() {
        // release
        INSTANCE = null;
        this.rocketMQProtocolServer = null;
    }

    @Test
    public void testRocketmqProtocolServer() {
        RocketMQProtocolServer rocketMQProtocolServer = new RocketMQProtocolServer();

        //set params
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("namespace", "");
        paramMap.put("customizedTraceTopic", "topic-dubbo-rpc");
        paramMap.put("corethreads", "1");
        paramMap.put("threads", "1");

        // set serverUrl
        URL serverUrl = URL.valueOf(ROCKETMQ_URL_TEMPLATE + "?" + mapToString(paramMap));
        rocketMQProtocolServer.reset(serverUrl);
        rocketMQProtocolServer.setMessageListenerConcurrently(INSTANCE.getMessageListenerConcurrently());
        try {
            rocketMQProtocolServer.createProducer();
            //assert
            Assertions.assertNotNull(rocketMQProtocolServer.getDefaultMQProducer());
            Assertions.assertNotNull(rocketMQProtocolServer.getDefaultMQPushConsumer());
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }

        this.rocketMQProtocolServer = rocketMQProtocolServer;
    }

    @Test
    public void testExport() {
        //set params
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("version", "4.9.2");
        paramMap.put("brokerName", "broker-a");
        paramMap.put("group", "DEFAULT_GROUP");
        paramMap.put("groupModel", "select");
        paramMap.put("topic", "dubbo_rpc_topic");
        paramMap.put("queueId", "");
        paramMap.put("corethreads", "1");
        paramMap.put("threads", "1");

        registryUrl = URL.valueOf(ROCKETMQ_URL_TEMPLATE + "?" + mapToString(paramMap));
        RocketMQProtocol rocketMQProtocol = new RocketMQProtocol();
        Invoker invoker = new RocketMQInvoker<>(RocketMQInvoker.class, registryUrl, this.rocketMQProtocolServer);

        //assert
        Assertions.assertNotNull(rocketMQProtocol.export(invoker));
    }

    public static String mapToString(Map<String, String> paramMap) {
        StringBuilder parameters = new StringBuilder();
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            parameters.append(entry.getKey());
            parameters.append("=");
            parameters.append(entry.getValue());
            parameters.append("&;");
        }
        return parameters.toString();
    }

}
