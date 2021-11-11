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
package org.apache.dubbo.config.bootstrap.builders;

import org.apache.dubbo.config.ProviderConfig;

/**
 * This is a builder for build {@link ProviderConfig}.
 *
 * @since 2.7
 */
public class ProviderBuilder extends AbstractServiceBuilder<ProviderConfig, ProviderBuilder> {

    /**
     * Service ip addresses (used when there are multiple network cards available)
     */
    private String host;

    /**
     * Service port
     */
    private Integer port;

    /**
     * Context path
     */
    private String contextpath;

    /**
     * Thread pool
     */
    private String threadpool;

    /**
     * Thread pool size (fixed size)
     */
    private Integer threads;

    /**
     * IO thread pool size (fixed size)
     */
    private Integer iothreads;

    /**
     * Thread pool queue length
     */
    private Integer queues;

    /**
     * Max acceptable connections
     */
    private Integer accepts;

    /**
     * Protocol codec
     */
    private String codec;

    /**
     * The serialization charset
     */
    private String charset;

    /**
     * Payload max length
     */
    private Integer payload;

    /**
     * The network io buffer size
     */
    private Integer buffer;

    /**
     * Transporter
     */
    private String transporter;

    /**
     * How information gets exchanged
     */
    private String exchanger;

    /**
     * Thread dispatching mode
     */
    private String dispatcher;

    /**
     * Networker
     */
    private String networker;

    /**
     * The server-side implementation model of the protocol
     */
    private String server;

    /**
     * The client-side implementation model of the protocol
     */
    private String client;

    /**
     * Supported telnet commands, separated with comma.
     */
    private String telnet;

    /**
     * Command line prompt
     */
    private String prompt;

    /**
     * Status check
     */
    private String status;

    /**
     * Wait time when stop
     */
    private Integer wait;

    /**
     * Whether to use the default protocol
     */
    private Boolean isDefault;

    public ProviderBuilder host(String host) {
        this.host = host;
        return getThis();
    }

    public ProviderBuilder port(Integer port) {
        this.port = port;
        return getThis();
    }

    public ProviderBuilder contextPath(String contextPath) {
        this.contextpath = contextPath;
        return getThis();
    }

    public ProviderBuilder threadPool(String threadPool) {
        this.threadpool = threadPool;
        return getThis();
    }

    public ProviderBuilder threads(Integer threads) {
        this.threads = threads;
        return getThis();
    }

    public ProviderBuilder ioThreads(Integer ioThreads) {
        this.iothreads = ioThreads;
        return getThis();
    }

    public ProviderBuilder queues(Integer queues) {
        this.queues = queues;
        return getThis();
    }

    public ProviderBuilder accepts(Integer accepts) {
        this.accepts = accepts;
        return getThis();
    }

    public ProviderBuilder codec(String codec) {
        this.codec = codec;
        return getThis();
    }

    public ProviderBuilder charset(String charset) {
        this.charset = charset;
        return getThis();
    }

    public ProviderBuilder payload(Integer payload) {
        this.payload = payload;
        return getThis();
    }

    public ProviderBuilder buffer(Integer buffer) {
        this.buffer = buffer;
        return getThis();
    }

    public ProviderBuilder transporter(String transporter) {
        this.transporter = transporter;
        return getThis();
    }

    public ProviderBuilder exchanger(String exchanger) {
        this.exchanger = exchanger;
        return getThis();
    }

    public ProviderBuilder dispatcher(String dispatcher) {
        this.dispatcher = dispatcher;
        return getThis();
    }

    public ProviderBuilder networker(String networker) {
        this.networker = networker;
        return getThis();
    }

    public ProviderBuilder server(String server) {
        this.server = server;
        return getThis();
    }

    public ProviderBuilder client(String client) {
        this.client = client;
        return getThis();
    }

    public ProviderBuilder telnet(String telnet) {
        this.telnet = telnet;
        return getThis();
    }

    public ProviderBuilder prompt(String prompt) {
        this.prompt = prompt;
        return getThis();
    }

    public ProviderBuilder status(String status) {
        this.status = status;
        return getThis();
    }

    public ProviderBuilder wait(Integer wait) {
        this.wait = wait;
        return getThis();
    }

    public ProviderBuilder isDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return getThis();
    }

    public ProviderConfig build() {
        ProviderConfig provider = new ProviderConfig();
        super.build(provider);

        provider.setHost(host);
        provider.setPort(port);
        provider.setContextpath(contextpath);
        provider.setThreadpool(threadpool);
        provider.setThreads(threads);
        provider.setIothreads(iothreads);
        provider.setQueues(queues);
        provider.setAccepts(accepts);
        provider.setCodec(codec);
        provider.setPayload(payload);
        provider.setCharset(charset);
        provider.setBuffer(buffer);
        provider.setTransporter(transporter);
        provider.setExchanger(exchanger);
        provider.setDispatcher(dispatcher);
        provider.setNetworker(networker);
        provider.setServer(server);
        provider.setClient(client);
        provider.setTelnet(telnet);
        provider.setPrompt(prompt);
        provider.setStatus(status);
        provider.setWait(wait);
        provider.setDefault(isDefault);

        return provider;
    }

    @Override
    protected ProviderBuilder getThis() {
        return this;
    }
}
