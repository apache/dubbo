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
package org.apache.dubbo.rpc.protocol.tri.h3.negotiation;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.rpc.protocol.tri.TripleConstants;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROTOCOL_ERROR_CLOSE_CLIENT;

public class AutoSwitchConnectionClient extends AbstractConnectionClient {

    private static final int MAX_RETRIES = 8;

    private final URL url;
    private final AbstractConnectionClient connectionClient;

    private AbstractConnectionClient http3ConnectionClient;
    private ScheduledExecutorService executor;
    private NegotiateClientCall clientCall;
    private boolean negotiated = false;
    private boolean http3Connected = false;
    private final AtomicBoolean scheduling = new AtomicBoolean();
    private int attempt = 0;

    public AutoSwitchConnectionClient(URL url, AbstractConnectionClient connectionClient) {
        this.url = url;
        this.connectionClient = connectionClient;
        executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Dubbo-http3-negotiation"));
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        connectionClient.addConnectedListener(() -> ClassUtils.runWith(tccl, () -> executor.execute(this::negotiate)));
        increase();
        if (logger.isInfoEnabled()) {
            logger.info(
                    "Start HTTP/3 AutoSwitchConnectionClient {} connect to the server {}",
                    NetUtils.getLocalAddress(),
                    url.toInetSocketAddress());
        }
    }

    private String getBaseUrl() {
        boolean ssl = url.getParameter(CommonConstants.SSL_ENABLED_KEY, false);
        CharSequence scheme = ssl ? TripleConstants.HTTPS_SCHEME : TripleConstants.HTTP_SCHEME;
        return scheme + "://" + url.getHost() + ':' + url.getPort() + '/';
    }

    private void negotiate() {
        if (negotiated) {
            return;
        }
        scheduling.set(false);
        if (clientCall == null) {
            clientCall = new NegotiateClientCall(connectionClient, executor);
        }
        logger.info("Start HTTP/3 negotiation for [{}]", getBaseUrl());
        clientCall.start(url).whenComplete((altSvc, t) -> {
            if (t == null) {
                if (altSvc.contains("h3=")) {
                    negotiateSuccess();
                    return;
                }
                logger.info(
                        "HTTP/3 negotiation succeed, but provider reply alt-svc='{}' not support HTTP/3 for [{}]",
                        altSvc,
                        getBaseUrl());
                return;
            }
            if (scheduling.compareAndSet(false, true)) {
                reScheduleNegotiate(t);
            }
        });
    }

    private void negotiateSuccess() {
        negotiated = true;
        logger.info("HTTP/3 negotiation succeed for [{}], create http3 client", getBaseUrl());
        http3ConnectionClient = Helper.createHttp3Client(url, connectionClient.getDelegateHandler());
        http3ConnectionClient.addConnectedListener(() -> setHttp3Connected(true));
        http3ConnectionClient.addDisconnectedListener(() -> setHttp3Connected(false));
        negotiateEnd();
    }

    private void reScheduleNegotiate(Throwable t) {
        if (attempt++ < MAX_RETRIES) {
            int delay = 1 << attempt + 2;
            logger.info("HTTP/3 negotiation failed, retry after {} seconds for [{}]", delay, getBaseUrl(), t);
            executor.schedule(this::negotiate, delay, TimeUnit.SECONDS);
            return;
        }
        logger.warn(
                PROTOCOL_ERROR_CLOSE_CLIENT,
                "",
                "",
                "Max retries " + MAX_RETRIES + " reached, HTTP/3 negotiation failed for " + getBaseUrl(),
                t);
        negotiateEnd();
    }

    private void negotiateEnd() {
        scheduling.set(false);
        executor.shutdown();
        executor = null;
        clientCall = null;
    }

    private void setHttp3Connected(boolean http3Connected) {
        this.http3Connected = http3Connected;
        logger.info("Switch protocol to {} for [{}]", http3Connected ? "HTTP/3" : "HTTP/2", url.toString(""));
    }

    public boolean isHttp3Connected() {
        return http3Connected;
    }

    @Override
    public boolean isConnected() {
        return http3Connected ? http3ConnectionClient.isConnected() : connectionClient.isConnected();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return http3Connected ? http3ConnectionClient.getLocalAddress() : connectionClient.getLocalAddress();
    }

    @Override
    public boolean release() {
        try {
            connectionClient.release();
        } catch (Throwable t) {
            logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", t.getMessage(), t);
        }
        if (http3ConnectionClient != null) {
            try {
                http3ConnectionClient.release();
            } catch (Throwable t) {
                logger.warn(PROTOCOL_ERROR_CLOSE_CLIENT, "", "", t.getMessage(), t);
            }
        }
        return true;
    }

    @Override
    protected void initConnectionClient() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAvailable() {
        return http3Connected ? http3ConnectionClient.isAvailable() : connectionClient.isAvailable();
    }

    @Override
    public void addCloseListener(Runnable func) {
        connectionClient.addCloseListener(func);
    }

    @Override
    public void addConnectedListener(Runnable func) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDisconnectedListener(Runnable func) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onConnected(Object channel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onGoaway(Object channel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroy() {
        connectionClient.destroy();
        if (http3ConnectionClient != null) {
            http3ConnectionClient.destroy();
        }
    }

    @Override
    public <T> T getChannel(Boolean generalizable) {
        return http3Connected
                ? http3ConnectionClient.getChannel(generalizable)
                : connectionClient.getChannel(generalizable);
    }

    @Override
    protected void doOpen() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doClose() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doConnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDisConnect() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Channel getChannel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "AutoSwitchConnectionClient{" + "http3Enabled=" + http3Connected + ", http3=" + http3ConnectionClient
                + ", http2=" + connectionClient + '}';
    }
}
