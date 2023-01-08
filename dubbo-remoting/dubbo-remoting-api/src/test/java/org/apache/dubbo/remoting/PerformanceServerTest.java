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
package org.apache.dubbo.remoting;

import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.serialize.support.DefaultSerializationSelector;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeServer;
import org.apache.dubbo.remoting.exchange.Exchangers;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import org.apache.dubbo.remoting.transport.dispatcher.execution.ExecutionDispatcher;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_THREADPOOL;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_THREADS;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADPOOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.CONFIG_UNDEFINED_ARGUMENT;
import static org.apache.dubbo.remoting.Constants.BUFFER_KEY;
import static org.apache.dubbo.remoting.Constants.DEFAULT_BUFFER_SIZE;

/**
 * PerformanceServer
 * <p>
 * mvn clean test -Dtest=*PerformanceServerTest -Dport=9911
 */
class PerformanceServerTest {

    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(PerformanceServerTest.class);
    private static ExchangeServer server = null;

    private static void restartServer(int times, int alive, int sleep) throws Exception {
        if (server != null && !server.isClosed()) {
            server.close();
            Thread.sleep(100);
        }

        for (int i = 0; i < times; i++) {
            logger.info("restart times:" + i);
            server = statServer();
            if (alive > 0) Thread.sleep(alive);
            server.close();
            if (sleep > 0) Thread.sleep(sleep);
        }

        server = statServer();
    }

    private static ExchangeServer statServer() throws Exception {
        final int port = PerformanceUtils.getIntProperty("port", 9911);
        final String transporter = PerformanceUtils.getProperty(Constants.TRANSPORTER_KEY, Constants.DEFAULT_TRANSPORTER);
        final String serialization = PerformanceUtils.getProperty(Constants.SERIALIZATION_KEY, DefaultSerializationSelector.getDefaultRemotingSerialization());
        final String threadpool = PerformanceUtils.getProperty(THREADPOOL_KEY, DEFAULT_THREADPOOL);
        final int threads = PerformanceUtils.getIntProperty(THREADS_KEY, DEFAULT_THREADS);
        final int iothreads = PerformanceUtils.getIntProperty(IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS);
        final int buffer = PerformanceUtils.getIntProperty(BUFFER_KEY, DEFAULT_BUFFER_SIZE);
        final String channelHandler = PerformanceUtils.getProperty(Constants.DISPATCHER_KEY, ExecutionDispatcher.NAME);


        // Start server
        ExchangeServer server = Exchangers.bind("exchange://0.0.0.0:" + port + "?transporter="
            + transporter + "&serialization="
            + serialization + "&threadpool=" + threadpool
            + "&threads=" + threads + "&iothreads=" + iothreads + "&buffer=" + buffer + "&channel.handler=" + channelHandler, new ExchangeHandlerAdapter(FrameworkModel.defaultModel()) {
            public String telnet(Channel channel, String message) throws RemotingException {
                return "echo: " + message + "\r\ntelnet> ";
            }

            public CompletableFuture<Object> reply(ExchangeChannel channel, Object request) throws RemotingException {
                if ("environment".equals(request)) {
                    return CompletableFuture.completedFuture(PerformanceUtils.getEnvironment());
                }
                if ("scene".equals(request)) {
                    List<String> scene = new ArrayList<String>();
                    scene.add("Transporter: " + transporter);
                    scene.add("Service Threads: " + threads);
                    return CompletableFuture.completedFuture(scene);
                }
                return CompletableFuture.completedFuture(request);
            }
        });

        return server;
    }

    private static ExchangeServer statTelnetServer(int port) throws Exception {
        // Start server
        ExchangeServer telnetserver = Exchangers.bind("exchange://0.0.0.0:" + port, new ExchangeHandlerAdapter(FrameworkModel.defaultModel()) {
            public String telnet(Channel channel, String message) throws RemotingException {
                if (message.equals("help")) {
                    return "support cmd: \r\n\tstart \r\n\tstop \r\n\tshutdown \r\n\trestart times [alive] [sleep] \r\ntelnet>";
                } else if (message.equals("stop")) {
                    logger.info("server closed:" + server);
                    server.close();
                    return "stop server\r\ntelnet>";
                } else if (message.startsWith("start")) {
                    try {
                        restartServer(0, 0, 0);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "start server\r\ntelnet>";
                } else if (message.startsWith("shutdown")) {
                    System.exit(0);
                    return "start server\r\ntelnet>";
                } else if (message.startsWith("channels")) {
                    return "server.getExchangeChannels():" + server.getExchangeChannels().size() + "\r\ntelnet>";
                } else if (message.startsWith("restart ")) { //r times [sleep] r 10 or r 10 100
                    String[] args = message.split(" ");
                    int times = Integer.parseInt(args[1]);
                    int alive = args.length > 2 ? Integer.parseInt(args[2]) : 0;
                    int sleep = args.length > 3 ? Integer.parseInt(args[3]) : 100;
                    try {
                        restartServer(times, alive, sleep);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return "restart server,times:" + times + " stop alive time: " + alive + ",sleep time: " + sleep + " usage:r times [alive] [sleep] \r\ntelnet>";
                } else {
                    return "echo: " + message + "\r\ntelnet> ";
                }

            }
        });

        return telnetserver;
    }

    @Test
    void testServer() throws Exception {
        // Read port from property
        if (PerformanceUtils.getProperty("port", null) == null) {
            logger.warn(CONFIG_UNDEFINED_ARGUMENT, "", "", "Please set -Dport=9911");
            return;
        }
        final int port = PerformanceUtils.getIntProperty("port", 9911);
        final boolean telnet = PerformanceUtils.getBooleanProperty("telnet", true);
        if (telnet) statTelnetServer(port + 1);
        server = statServer();

        synchronized (PerformanceServerTest.class) {
            while (true) {
                try {
                    PerformanceServerTest.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
