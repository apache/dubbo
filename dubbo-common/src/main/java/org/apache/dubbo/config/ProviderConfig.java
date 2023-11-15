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
package org.apache.dubbo.config;

import org.apache.dubbo.config.support.Parameter;
import org.apache.dubbo.rpc.model.ModuleModel;

import java.util.ArrayList;
import java.util.Arrays;

import static org.apache.dubbo.common.constants.CommonConstants.EXPORT_BACKGROUND_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.EXPORT_THREAD_NUM_KEY;

/**
 * Configuration for the service provider.
 *
 * @export
 * @see org.apache.dubbo.config.ProtocolConfig
 * @see ServiceConfigBase
 */
public class ProviderConfig extends AbstractServiceConfig {

    private static final long serialVersionUID = 6913423882496634749L;

    /* ======== Default values for protocols, which take effect when protocol attributes are not set ======== */

    /**
     * The IP addresses of the service (used when there are multiple network cards available).
     */
    private String host;

    /**
     * The port of the service.
     */
    private Integer port;

    /**
     * The context path of the service.
     */
    private String contextpath;

    /**
     * The thread pool configuration.
     */
    private String threadpool;

    /**
     * The name of the thread pool.
     */
    private String threadname;

    /**
     * The size of the thread pool (fixed size).
     */
    private Integer threads;

    /**
     * The size of the I/O thread pool (fixed size).
     */
    private Integer iothreads;

    /**
     * The keep-alive time of the thread pool, default unit: TimeUnit.MILLISECONDS.
     */
    private Integer alive;

    /**
     * The length of the thread pool queue.
     */
    private Integer queues;

    /**
     * The maximum number of acceptable connections.
     */
    private Integer accepts;

    /**
     * The codec used by the protocol.
     */
    private String codec;

    /**
     * The charset used for serialization.
     */
    private String charset;

    /**
     * The maximum payload length.
     */
    private Integer payload;

    /**
     * The size of the network I/O buffer.
     */
    private Integer buffer;

    /**
     * The transporter used by the protocol.
     */
    private String transporter;

    /**
     * The method of information exchange.
     */
    private String exchanger;

    /**
     * The mode of thread dispatching.
     */
    private String dispatcher;

    /**
     * The networker used by the protocol.
     */
    private String networker;

    /**
     * The server-side implementation model of the protocol.
     */
    private String server;

    /**
     * The client-side implementation model of the protocol.
     */
    private String client;

    /**
     * Supported telnet commands, separated by commas.
     */
    private String telnet;

    /**
     * The command line prompt.
     */
    private String prompt;

    /**
     * The status check configuration.
     */
    private String status;

    /**
     * The wait time when stopping the service.
     */
    private Integer wait;

    /**
     * The number of threads for the asynchronous export pool.
     */
    private Integer exportThreadNum;

    /**
     * Whether the export should run in the background or not.
     *
     * @deprecated Replace with {@link ModuleConfig#setBackground(Boolean)}
     * @see ModuleConfig#setBackground(Boolean)
     */
    private Boolean exportBackground;

    public ProviderConfig() {}

    public ProviderConfig(ModuleModel moduleModel) {
        super(moduleModel);
    }

    @Deprecated
    public void setProtocol(String protocol) {
        this.protocols = new ArrayList<>(Arrays.asList(new ProtocolConfig(protocol)));
    }

    @Parameter(excluded = true)
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Parameter(excluded = true)
    public Integer getPort() {
        return port;
    }

    @Deprecated
    public void setPort(Integer port) {
        this.port = port;
    }

    @Deprecated
    @Parameter(excluded = true, attribute = false)
    public String getPath() {
        return getContextpath();
    }

    @Deprecated
    public void setPath(String path) {
        setContextpath(path);
    }

    @Parameter(excluded = true)
    public String getContextpath() {
        return contextpath;
    }

    public void setContextpath(String contextpath) {
        this.contextpath = contextpath;
    }

    public String getThreadpool() {
        return threadpool;
    }

    public void setThreadpool(String threadpool) {
        this.threadpool = threadpool;
    }

    public String getThreadname() {
        return threadname;
    }

    public void setThreadname(String threadname) {
        this.threadname = threadname;
    }

    public Integer getThreads() {
        return threads;
    }

    public void setThreads(Integer threads) {
        this.threads = threads;
    }

    public Integer getIothreads() {
        return iothreads;
    }

    public void setIothreads(Integer iothreads) {
        this.iothreads = iothreads;
    }

    public Integer getAlive() {
        return alive;
    }

    public void setAlive(Integer alive) {
        this.alive = alive;
    }

    public Integer getQueues() {
        return queues;
    }

    public void setQueues(Integer queues) {
        this.queues = queues;
    }

    public Integer getAccepts() {
        return accepts;
    }

    public void setAccepts(Integer accepts) {
        this.accepts = accepts;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public Integer getPayload() {
        return payload;
    }

    public void setPayload(Integer payload) {
        this.payload = payload;
    }

    public Integer getBuffer() {
        return buffer;
    }

    public void setBuffer(Integer buffer) {
        this.buffer = buffer;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getTelnet() {
        return telnet;
    }

    public void setTelnet(String telnet) {
        this.telnet = telnet;
    }

    @Parameter(escaped = true)
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransporter() {
        return transporter;
    }

    public void setTransporter(String transporter) {
        this.transporter = transporter;
    }

    public String getExchanger() {
        return exchanger;
    }

    public void setExchanger(String exchanger) {
        this.exchanger = exchanger;
    }

    /**
     * typo, switch to use {@link #getDispatcher()}
     *
     * @deprecated {@link #getDispatcher()}
     */
    @Deprecated
    @Parameter(excluded = true, attribute = false)
    public String getDispather() {
        return getDispatcher();
    }

    /**
     * typo, switch to use {@link #getDispatcher()}
     *
     * @deprecated {@link #setDispatcher(String)}
     */
    @Deprecated
    public void setDispather(String dispather) {
        setDispatcher(dispather);
    }

    public String getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(String dispatcher) {
        this.dispatcher = dispatcher;
    }

    public String getNetworker() {
        return networker;
    }

    public void setNetworker(String networker) {
        this.networker = networker;
    }

    public Integer getWait() {
        return wait;
    }

    public void setWait(Integer wait) {
        this.wait = wait;
    }

    @Deprecated
    @Parameter(key = EXPORT_THREAD_NUM_KEY, excluded = true)
    public Integer getExportThreadNum() {
        return exportThreadNum;
    }

    @Deprecated
    public void setExportThreadNum(Integer exportThreadNum) {
        this.exportThreadNum = exportThreadNum;
    }

    /**
     * @deprecated replace with {@link ModuleConfig#getBackground()}
     * @see ModuleConfig#getBackground()
     */
    @Deprecated
    @Parameter(key = EXPORT_BACKGROUND_KEY, excluded = true)
    public Boolean getExportBackground() {
        return exportBackground;
    }

    /**
     * Whether export should run in background or not.
     *
     * @deprecated replace with {@link ModuleConfig#setBackground(Boolean)}
     * @see ModuleConfig#setBackground(Boolean)
     */
    @Deprecated
    public void setExportBackground(Boolean exportBackground) {
        this.exportBackground = exportBackground;
    }
}
