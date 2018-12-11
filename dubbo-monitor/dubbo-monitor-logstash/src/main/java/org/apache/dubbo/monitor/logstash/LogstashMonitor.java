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
package org.apache.dubbo.monitor.logstash;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.ssl.SslHandler;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.monitor.Monitor;
import org.apache.dubbo.monitor.MonitorService;
import org.apache.dubbo.monitor.dubbo.Statistics;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class LogstashMonitor implements Monitor {

    private static final Logger logger = LoggerFactory.getLogger(LogstashMonitor.class);
    private static final int LENGTH = 10;
    private final long monitorInterval;
    private final long reconnectInterval;
    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newScheduledThreadPool(3,
                    new NamedThreadFactory("LogstashMonitorSendTimer", true));
    private final ScheduledFuture<?> sendFuture;
    private final ConcurrentMap<Statistics, AtomicReference<long[]>> statisticsMap = new ConcurrentHashMap<>();
    private final URL url;
    private final Bootstrap bootstrap;
    private final EventLoopGroup group = new NioEventLoopGroup();
    private ChannelFuture connect;

    LogstashMonitor(URL url) {
        this.url = url;
        this.monitorInterval = url.getPositiveParameter("interval", 60000);
        this.reconnectInterval = url.getPositiveParameter("reconnect", 60);
        this.bootstrap = buildBootstrap();

        // connect to logstash server
        doConnect();

        // collect timer for collecting statistics data
        sendFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            // collect data
            try {
                send();
            } catch (Throwable t) {
                logger.error("Unexpected error occur at send statistic, cause: " + t.getMessage(), t);
            }
        }, monitorInterval, monitorInterval, TimeUnit.MILLISECONDS);
    }

    public void send() {
        logger.debug("Send statistics to monitor " + getUrl());
        long timestamp = System.currentTimeMillis();
        for (Map.Entry<Statistics, AtomicReference<long[]>> entry : statisticsMap.entrySet()) {
            // get statistics data
            Statistics statistics = entry.getKey();
            AtomicReference<long[]> reference = entry.getValue();
            long[] numbers = reference.get();

            // send statistics data
            MonitorData data = new MonitorData();
            data.timestamp = timestamp;
            data.url = statistics.getUrl();
            data.date = new Date(timestamp);

            data.application = statistics.getApplication();
            data.group = statistics.getGroup();
            data.client = statistics.getClient();
            data.server = statistics.getServer();
            data.method = statistics.getMethod();
            data.service = statistics.getService();
            data.version = statistics.getVersion();

            data.version = data.url.getParameter(Constants.DEFAULT_PROTOCOL);
            data.success = numbers[0];
            data.failure = numbers[1];
            data.input = numbers[2];
            data.output = numbers[3];
            data.elapsed = numbers[4];
            data.concurrent = numbers[5];
            data.maxInput = numbers[6];
            data.maxOutput = numbers[7];
            data.maxElapsed = numbers[8];
            data.maxConcurrent = numbers[9];

            writeEvent(data);

            // reset
            long[] current;
            long[] update = new long[LENGTH];
            do {
                current = reference.get();
                if (current == null) {
                    update[0] = 0;
                    update[1] = 0;
                    update[2] = 0;
                    update[3] = 0;
                    update[4] = 0;
                    update[5] = 0;
                } else {
                    update[0] = current[0] - data.success;
                    update[1] = current[1] - data.failure;
                    update[2] = current[2] - data.input;
                    update[3] = current[3] - data.output;
                    update[4] = current[4] - data.elapsed;
                    update[5] = current[5] - data.concurrent;
                }
            } while (!reference.compareAndSet(current, update));
        }
    }

    private Bootstrap buildBootstrap() {
        String format = url.getParameter("format", "json");
        boolean ssl = url.getParameter("ssl", false);
        MessageToByteEncoder encoder;
        switch (format) {
            case "json":
                encoder = new JsonMonitorDataEncoder();
                break;
            case "plain":
                encoder = new PlainMonitorDataEncoder();
                break;
            default:
                encoder = new JsonMonitorDataEncoder();
                logger.warn(String.format("Unknown event format: %s, will use default \"json\"", format));
        }
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                if (ssl) {
                    ch.pipeline().addFirst("ssl", getSslHandler());
                }
                ch.pipeline().addLast("reconnect", new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                        super.channelInactive(ctx);
                        logger.warn("channel inactive, will schedule for reconnect");
                        ctx.channel().eventLoop().schedule(() -> doConnect(), reconnectInterval, TimeUnit.SECONDS);
                    }
                }).addLast("decoder", encoder);
            }
        });
        return bootstrap;
    }

    private SslHandler getSslHandler() throws IOException, GeneralSecurityException {
        KeyManagerFactory keyManagerFactory = null;
        String keyStorePath = url.getParameter("keyStorePath");
        char[] keyStorePass = url.getParameter("keyStorePass") == null ?
                null : url.getParameter("keyStorePass").toCharArray();
        if (keyStorePath != null && !keyStorePath.isEmpty()) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (InputStream is = new FileInputStream(keyStorePath)) {
                keyStore.load(is, keyStorePass == null ? null : keyStorePass);
            }
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePass);
        }

        TrustManagerFactory trustManagerFactory = null;
        String trustStorePath = url.getParameter("trustStorePath");
        char[] trustStorePass = url.getParameter("trustStorePass") == null ?
                null : url.getParameter("trustStorePass").toCharArray();
        if (trustStorePath != null && !trustStorePath.isEmpty()) {
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (InputStream is = new FileInputStream(trustStorePath)) {
                trustStore.load(is, trustStorePass);
            }
            trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
        }

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(),
                trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers(), null);
        SSLEngine engine = context.createSSLEngine();
        engine.setUseClientMode(true);

        if (keyManagerFactory != null) {
            engine.setNeedClientAuth(true);
        }

        return new SslHandler(engine);
    }

    private void writeEvent(MonitorData data) {
        if (connect.channel().isActive()) {
            connect.channel().write(data);
            connect.channel().flush();
        }
    }

    private void doConnect() {
        connect = bootstrap.connect(url.getHost(), url.getPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                logger.debug("Connected to logstash server " + url.getHost() + ":" + url.getPort());
            } else {
                logger.warn("Connect to logstash server failed " + url.getHost() + ":" + url.getPort());
                future.channel().eventLoop().schedule(() -> doConnect(), reconnectInterval, TimeUnit.SECONDS);
            }
        });
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isAvailable() {
        return connect.channel().isActive();
    }

    @Override
    public void destroy() {
        try {
            sendFuture.cancel(true);
            group.shutdownGracefully();
        } catch (Throwable t) {
            logger.error("Unexpected error occur at cancel sender timer, cause: " + t.getMessage(), t);
        }
    }

    @Override
    public void collect(URL url) {
        // data to collect from url
        int success = url.getParameter(MonitorService.SUCCESS, 0);
        int failure = url.getParameter(MonitorService.FAILURE, 0);
        int input = url.getParameter(MonitorService.INPUT, 0);
        int output = url.getParameter(MonitorService.OUTPUT, 0);
        int elapsed = url.getParameter(MonitorService.ELAPSED, 0);
        int concurrent = url.getParameter(MonitorService.CONCURRENT, 0);
        // init atomic reference
        Statistics statistics = new Statistics(url);
        AtomicReference<long[]> reference = statisticsMap.get(statistics);
        if (reference == null) {
            statisticsMap.putIfAbsent(statistics, new AtomicReference<long[]>());
            reference = statisticsMap.get(statistics);
        }
        // use CompareAndSet to sum
        long[] current;
        long[] update = new long[LENGTH];
        do {
            current = reference.get();
            if (current == null) {
                update[0] = success;
                update[1] = failure;
                update[2] = input;
                update[3] = output;
                update[4] = elapsed;
                update[5] = concurrent;
                update[6] = input;
                update[7] = output;
                update[8] = elapsed;
                update[9] = concurrent;
            } else {
                update[0] = current[0] + success;
                update[1] = current[1] + failure;
                update[2] = current[2] + input;
                update[3] = current[3] + output;
                update[4] = current[4] + elapsed;
                update[5] = (current[5] + concurrent) / 2;
                update[6] = current[6] > input ? current[6] : input;
                update[7] = current[7] > output ? current[7] : output;
                update[8] = current[8] > elapsed ? current[8] : elapsed;
                update[9] = current[9] > concurrent ? current[9] : concurrent;
            }
        } while (!reference.compareAndSet(current, update));
    }

    @Override
    public List<URL> lookup(URL query) {
        return null;
    }
}
