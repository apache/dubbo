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
package org.apache.dubbo.config.builders;

import org.apache.dubbo.config.ProtocolConfig;

import java.util.Map;

/**
 * This is a builder for build {@link ProtocolConfig}.
 *
 * @since 2.7
 */
public class ProtocolBuilder extends AbstractBuilder<ProtocolConfig, ProtocolBuilder> {
    /**
     * Protocol name
     */
    private String name;

    /**
     * Service ip address (when there are multiple network cards available)
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
     * Thread pool core thread size
     */
    private Integer corethreads;

    /**
     * Thread pool size (fixed size)
     */
    private Integer threads;

    /**
     * IO thread pool size (fixed size)
     */
    private Integer iothreads;

    /**
     * Thread pool's queue length
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
     * Serialization
     */
    private String serialization;

    /**
     * Charset
     */
    private String charset;

    /**
     * Payload max length
     */
    private Integer payload;

    /**
     * Buffer size
     */
    private Integer buffer;

    /**
     * Heartbeat interval
     */
    private Integer heartbeat;

    /**
     * Access log
     */
    private String accesslog;

    /**
     * Transfort
     */
    private String transporter;

    /**
     * How information is exchanged
     */
    private String exchanger;

    /**
     * Thread dispatch mode
     */
    private String dispatcher;

    /**
     * Networker
     */
    private String networker;

    /**
     * Sever impl
     */
    private String server;

    /**
     * Client impl
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
     * Whether to register
     */
    private Boolean register;

    /**
     * whether it is a persistent connection
     */
    //TODO add this to provider config
    private Boolean keepAlive;

    // TODO add this to provider config
    private String optimizer;

    /**
     * The extension
     */
    private String extension;

    /**
     * The customized parameters
     */
    private Map<String, String> parameters;

    /**
     * If it's default
     */
    private Boolean isDefault;

    public ProtocolBuilder name(String name) {
        this.name = name;
        return getThis();
    }

    public ProtocolBuilder host(String host) {
        this.host = host;
        return getThis();
    }

    public ProtocolBuilder port(Integer port) {
        this.port = port;
        return getThis();
    }

    public ProtocolBuilder contextpath(String contextpath) {
        this.contextpath = contextpath;
        return getThis();
    }

    /**
     * @see org.apache.dubbo.config.builders.ProtocolBuilder#contextpath(String)
     * @param path
     * @return ProtocolBuilder
     */
    @Deprecated
    public ProtocolBuilder path(String path) {
        this.contextpath = path;
        return getThis();
    }

    public ProtocolBuilder threadpool(String threadpool) {
        this.threadpool = threadpool;
        return getThis();
    }

    public ProtocolBuilder corethreads(Integer corethreads) {
        this.corethreads = corethreads;
        return getThis();
    }

    public ProtocolBuilder threads(Integer threads) {
        this.threads = threads;
        return getThis();
    }

    public ProtocolBuilder iothreads(Integer iothreads) {
        this.iothreads = iothreads;
        return getThis();
    }

    public ProtocolBuilder queues(Integer queues) {
        this.queues = queues;
        return getThis();
    }

    public ProtocolBuilder accepts(Integer accepts) {
        this.accepts = accepts;
        return getThis();
    }

    public ProtocolBuilder codec(String codec) {
        this.codec = codec;
        return getThis();
    }

    public ProtocolBuilder serialization(String serialization) {
        this.serialization = serialization;
        return getThis();
    }

    public ProtocolBuilder charset(String charset) {
        this.charset = charset;
        return getThis();
    }

    public ProtocolBuilder payload(Integer payload) {
        this.payload = payload;
        return getThis();
    }

    public ProtocolBuilder buffer(Integer buffer) {
        this.buffer = buffer;
        return getThis();
    }

    public ProtocolBuilder heartbeat(Integer heartbeat) {
        this.heartbeat = heartbeat;
        return getThis();
    }

    public ProtocolBuilder accesslog(String accesslog) {
        this.accesslog = accesslog;
        return getThis();
    }

    public ProtocolBuilder transporter(String transporter) {
        this.transporter = transporter;
        return getThis();
    }

    public ProtocolBuilder exchanger(String exchanger) {
        this.exchanger = exchanger;
        return getThis();
    }

    public ProtocolBuilder dispatcher(String dispatcher) {
        this.dispatcher = dispatcher;
        return getThis();
    }

    /**
     * @see org.apache.dubbo.config.builders.ProtocolBuilder#dispatcher(String)
     * @param dispather
     * @return ProtocolBuilder
     */
    @Deprecated
    public ProtocolBuilder dispather(String dispather) {
        this.dispatcher = dispather;
        return getThis();
    }

    public ProtocolBuilder networker(String networker) {
        this.networker = networker;
        return getThis();
    }

    public ProtocolBuilder server(String server) {
        this.server = server;
        return getThis();
    }

    public ProtocolBuilder client(String client) {
        this.client = client;
        return getThis();
    }

    public ProtocolBuilder telnet(String telnet) {
        this.telnet = telnet;
        return getThis();
    }

    public ProtocolBuilder prompt(String prompt) {
        this.prompt = prompt;
        return getThis();
    }

    public ProtocolBuilder status(String status) {
        this.status = status;
        return getThis();
    }

    public ProtocolBuilder register(Boolean register) {
        this.register = register;
        return getThis();
    }

    public ProtocolBuilder keepAlive(Boolean keepAlive) {
        this.keepAlive = keepAlive;
        return getThis();
    }

    public ProtocolBuilder optimizer(String optimizer) {
        this.optimizer = optimizer;
        return getThis();
    }

    public ProtocolBuilder extension(String extension) {
        this.extension = extension;
        return getThis();
    }

    public ProtocolBuilder appendParameter(String key, String value) {
        this.parameters = appendParameter(parameters, key, value);
        return getThis();
    }

    public ProtocolBuilder appendParameters(Map<String, String> appendParameters) {
        this.parameters = appendParameters(parameters, appendParameters);
        return getThis();
    }

    public ProtocolBuilder isDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return getThis();
    }

    public ProtocolConfig build() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        super.build(protocolConfig);

        protocolConfig.setAccepts(accepts);
        protocolConfig.setAccesslog(accesslog);
        protocolConfig.setBuffer(buffer);
        protocolConfig.setCharset(charset);
        protocolConfig.setClient(client);
        protocolConfig.setCodec(codec);
        protocolConfig.setContextpath(contextpath);
        protocolConfig.setCorethreads(corethreads);
        protocolConfig.setDefault(isDefault);
        protocolConfig.setDispatcher(dispatcher);
        protocolConfig.setExchanger(exchanger);
        protocolConfig.setExtension(extension);
        protocolConfig.setHeartbeat(heartbeat);
        protocolConfig.setHost(host);
        protocolConfig.setIothreads(iothreads);
        protocolConfig.setKeepAlive(keepAlive);
        protocolConfig.setName(name);
        protocolConfig.setNetworker(networker);
        protocolConfig.setOptimizer(optimizer);
        protocolConfig.setParameters(parameters);
        protocolConfig.setPayload(payload);
        protocolConfig.setPort(port);
        protocolConfig.setPrompt(prompt);
        protocolConfig.setQueues(queues);
        protocolConfig.setRegister(register);
        protocolConfig.setSerialization(serialization);
        protocolConfig.setServer(server);
        protocolConfig.setStatus(status);
        protocolConfig.setTelnet(telnet);
        protocolConfig.setThreadpool(threadpool);
        protocolConfig.setThreads(threads);
        protocolConfig.setTransporter(transporter);

        return protocolConfig;
    }

    @Override
    protected ProtocolBuilder getThis() {
        return this;
    }
}
