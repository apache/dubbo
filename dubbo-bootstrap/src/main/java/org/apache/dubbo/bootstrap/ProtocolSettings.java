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
package org.apache.dubbo.bootstrap;

import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.builders.ProtocolBuilder;

import java.util.Map;

/**
 * The settings of {@link ProtocolConfig protcol}
 *
 * @see ProtocolBuilder
 * @since 2.7.3
 */
public class ProtocolSettings extends AbstractSettings {

    private final ProtocolBuilder builder;

    public ProtocolSettings(ProtocolBuilder builder, DubboBootstrap dubboBootstrap) {
        super(dubboBootstrap);
        this.builder = builder;
    }

    public ProtocolSettings name(String name) {
        builder.name(name);
        return this;
    }

    public ProtocolSettings host(String host) {
        builder.host(host);
        return this;
    }

    public ProtocolSettings port(Integer port) {
        builder.port(port);
        return this;
    }

    public ProtocolSettings contextpath(String contextpath) {
        builder.contextpath(contextpath);
        return this;
    }

    @Deprecated
    public ProtocolSettings path(String path) {
        builder.path(path);
        return this;
    }

    public ProtocolSettings threadpool(String threadpool) {
        builder.threadpool(threadpool);
        return this;
    }

    public ProtocolSettings corethreads(Integer corethreads) {
        builder.corethreads(corethreads);
        return this;
    }

    public ProtocolSettings threads(Integer threads) {
        builder.threads(threads);
        return this;
    }

    public ProtocolSettings iothreads(Integer iothreads) {
        builder.iothreads(iothreads);
        return this;
    }

    public ProtocolSettings queues(Integer queues) {
        builder.queues(queues);
        return this;
    }

    public ProtocolSettings accepts(Integer accepts) {
        builder.accepts(accepts);
        return this;
    }

    public ProtocolSettings codec(String codec) {
        builder.codec(codec);
        return this;
    }

    public ProtocolSettings serialization(String serialization) {
        builder.serialization(serialization);
        return this;
    }

    public ProtocolSettings charset(String charset) {
        builder.charset(charset);
        return this;
    }

    public ProtocolSettings payload(Integer payload) {
        builder.payload(payload);
        return this;
    }

    public ProtocolSettings buffer(Integer buffer) {
        builder.buffer(buffer);
        return this;
    }

    public ProtocolSettings heartbeat(Integer heartbeat) {
        builder.heartbeat(heartbeat);
        return this;
    }

    public ProtocolSettings accesslog(String accesslog) {
        builder.accesslog(accesslog);
        return this;
    }

    public ProtocolSettings transporter(String transporter) {
        builder.transporter(transporter);
        return this;
    }

    public ProtocolSettings exchanger(String exchanger) {
        builder.exchanger(exchanger);
        return this;
    }

    public ProtocolSettings dispatcher(String dispatcher) {
        builder.dispatcher(dispatcher);
        return this;
    }

    @Deprecated
    public ProtocolSettings dispather(String dispather) {
        builder.dispather(dispather);
        return this;
    }

    public ProtocolSettings networker(String networker) {
        builder.networker(networker);
        return this;
    }

    public ProtocolSettings server(String server) {
        builder.server(server);
        return this;
    }

    public ProtocolSettings client(String client) {
        builder.client(client);
        return this;
    }

    public ProtocolSettings telnet(String telnet) {
        builder.telnet(telnet);
        return this;
    }

    public ProtocolSettings prompt(String prompt) {
        builder.prompt(prompt);
        return this;
    }

    public ProtocolSettings status(String status) {
        builder.status(status);
        return this;
    }

    public ProtocolSettings register(Boolean register) {
        builder.register(register);
        return this;
    }

    public ProtocolSettings keepAlive(Boolean keepAlive) {
        builder.keepAlive(keepAlive);
        return this;
    }

    public ProtocolSettings optimizer(String optimizer) {
        builder.optimizer(optimizer);
        return this;
    }

    public ProtocolSettings extension(String extension) {
        builder.extension(extension);
        return this;
    }

    public ProtocolSettings appendParameter(String key, String value) {
        builder.appendParameter(key, value);
        return this;
    }

    public ProtocolSettings appendParameters(Map<String, String> appendParameters) {
        builder.appendParameters(appendParameters);
        return this;
    }

    public ProtocolSettings isDefault(Boolean isDefault) {
        builder.isDefault(isDefault);
        return this;
    }
}
