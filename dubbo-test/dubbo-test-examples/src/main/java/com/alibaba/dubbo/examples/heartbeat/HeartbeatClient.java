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
package com.alibaba.dubbo.examples.heartbeat;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Transporters;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.exchange.ExchangeServer;
import com.alibaba.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import com.alibaba.dubbo.remoting.exchange.support.header.HeaderExchangeClient;
import com.alibaba.dubbo.remoting.exchange.support.header.HeaderExchangeServer;

public class HeartbeatClient {

    private static final URL serverUrl = URL.valueOf("netty://" + NetUtils.getLocalHost() + ":9999")
            .addParameter(Constants.CODEC_KEY, "exchange");

    private static final ExchangeHandler handler = new ExchangeHandlerAdapter() {

    };

    private static ExchangeServer exchangeServer;

    private static volatile boolean serverStarted = false;

    public static void main(String[] args) throws Exception {

        final HeartBeatExchangeHandler serverHandler = new HeartBeatExchangeHandler(handler);

        Thread serverThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    exchangeServer = new HeaderExchangeServer(
                            Transporters.bind(serverUrl, serverHandler));
                    serverStarted = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        serverThread.setDaemon(true);
        serverThread.start();

        while (!serverStarted) {
            Thread.sleep(1000);
        }

        URL url = serverUrl.addParameter(Constants.HEARTBEAT_KEY, 1000);

        HeartBeatExchangeHandler clientHandler = new HeartBeatExchangeHandler(handler);
        ExchangeClient exchangeClient = new HeaderExchangeClient(Transporters.connect(url, clientHandler),true);

        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            System.out.print(".");
        }

        System.out.println();

        if (serverHandler.getHeartBeatCount() > 0) {
            System.out.printf("Server receives %d heartbeats",
                    serverHandler.getHeartBeatCount());
        } else {
            throw new Exception("Client heartbeat does not work.");
        }

        exchangeClient.close();
        exchangeServer.close();

    }

}
