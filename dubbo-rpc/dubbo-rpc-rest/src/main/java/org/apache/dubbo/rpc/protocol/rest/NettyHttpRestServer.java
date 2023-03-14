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
package org.apache.dubbo.rpc.protocol.rest;

import io.netty.channel.ChannelOption;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.rest.PathMatcher;
import org.apache.dubbo.metadata.rest.RestMethodMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.rest.exception.mapper.ExceptionMapper;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyServer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_THREADS;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.THREADS_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.remoting.Constants.DEFAULT_IO_THREADS;
import static org.apache.dubbo.remoting.Constants.DEFAULT_PAYLOAD;
import static org.apache.dubbo.remoting.Constants.PAYLOAD_KEY;
import static org.apache.dubbo.rpc.protocol.rest.Constants.DEFAULT_KEEP_ALIVE;
import static org.apache.dubbo.rpc.protocol.rest.Constants.EXCEPTION_MAPPER_KEY;
import static org.apache.dubbo.rpc.protocol.rest.Constants.KEEP_ALIVE_KEY;


public class NettyHttpRestServer implements RestProtocolServer {
    private NettyServer server = getNettyServer();

    /**
     *  for triple override
     * @return
     */
    protected NettyServer getNettyServer() {
        return new NettyServer();
    }

    private String address;

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();


    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public void setAddress(String address) {

        this.address = address;
    }

    @Override
    public void close() {
        server.stop();

    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public void start(URL url) {

        registerExceptionMapper(url);

        String bindIp = url.getParameter(BIND_IP_KEY, url.getHost());
        if (!url.isAnyHost() && NetUtils.isValidLocalHost(bindIp)) {
            server.setHostname(bindIp);
        }
        server.setPort(url.getParameter(BIND_PORT_KEY, url.getPort()));

        Map<ChannelOption, Object> channelOption = new HashMap<>();
        channelOption.put(ChannelOption.SO_KEEPALIVE, url.getParameter(KEEP_ALIVE_KEY, DEFAULT_KEEP_ALIVE));
        server.setChildChannelOptions(channelOption);
        server.setExecutorThreadCount(url.getParameter(THREADS_KEY, DEFAULT_THREADS));
        server.setIoWorkerCount(url.getParameter(IO_THREADS_KEY, DEFAULT_IO_THREADS));
        server.setMaxRequestSize(url.getParameter(PAYLOAD_KEY, DEFAULT_PAYLOAD));
        server.start();
    }

    @Override
    public void deploy(Map<PathMatcher, RestMethodMetadata> metadataMap, Invoker invoker) {
        PathAndInvokerMapper.addPathAndInvoker(metadataMap, invoker);
    }

    @Override
    public void undeploy(PathMatcher pathMatcher) {
        PathAndInvokerMapper.removePath(pathMatcher);
    }

    private void registerExceptionMapper(URL url) {

        for (String clazz : COMMA_SPLIT_PATTERN.split(url.getParameter(EXCEPTION_MAPPER_KEY, RpcExceptionMapper.class.getName()))) {
            if (!StringUtils.isEmpty(clazz)) {
                ExceptionMapper.registerMapper(clazz);
            }
        }


    }


}
