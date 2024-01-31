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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.rest.ServiceRestMetadata;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.rest.constans.RestConstant;
import org.apache.dubbo.rpc.protocol.rest.deploy.ServiceDeployer;
import org.apache.dubbo.rpc.protocol.rest.netty.NettyServer;
import org.apache.dubbo.rpc.protocol.rest.netty.RestHttpRequestDecoder;
import org.apache.dubbo.rpc.protocol.rest.netty.UnSharedHandlerCreator;
import org.apache.dubbo.rpc.protocol.rest.netty.ssl.SslServerTlsHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import static org.apache.dubbo.common.constants.CommonConstants.BACKLOG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.IO_THREADS_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.remoting.Constants.DEFAULT_IO_THREADS;

/**
 * netty http server
 */
public class NettyHttpRestServer implements RestProtocolServer {

    private ServiceDeployer serviceDeployer = new ServiceDeployer();
    private NettyServer server = getNettyServer();

    /**
     * for triple override
     *
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

        registerExtension(url);

        String bindIp = url.getParameter(BIND_IP_KEY, url.getHost());
        if (!url.isAnyHost() && NetUtils.isValidLocalHost(bindIp)) {
            server.setHostname(bindIp);
        }
        server.setPort(url.getParameter(BIND_PORT_KEY, url.getPort()));

        // child options
        server.setChildChannelOptions(getChildChannelOptionMap(url));

        // set options
        server.setChannelOptions(getChannelOptionMap(url));
        // set unshared callback
        server.setUnSharedHandlerCallBack(getUnSharedHttpChannelHandlers());
        // set channel handler  and @Shared
        server.setChannelHandlers(getChannelHandlers(url));
        server.setIoWorkerCount(url.getParameter(IO_THREADS_KEY, DEFAULT_IO_THREADS));
        server.start(url);
    }

    private UnSharedHandlerCreator getUnSharedHttpChannelHandlers() {
        return new UnSharedHandlerCreator() {
            @Override
            public List<ChannelHandler> getUnSharedHandlers(URL url) {
                return Arrays.asList(
                        //  add SslServerTlsHandler
                        new SslServerTlsHandler(url),
                        new HttpRequestDecoder(
                                url.getParameter(
                                        RestConstant.MAX_INITIAL_LINE_LENGTH_PARAM,
                                        RestConstant.MAX_INITIAL_LINE_LENGTH),
                                url.getParameter(RestConstant.MAX_HEADER_SIZE_PARAM, RestConstant.MAX_HEADER_SIZE),
                                url.getParameter(RestConstant.MAX_CHUNK_SIZE_PARAM, RestConstant.MAX_CHUNK_SIZE)),
                        new HttpObjectAggregator(
                                url.getParameter(RestConstant.MAX_REQUEST_SIZE_PARAM, RestConstant.MAX_REQUEST_SIZE)),
                        new HttpResponseEncoder(),
                        new RestHttpRequestDecoder(url, serviceDeployer));
            }
        };
    }

    /**
     * create child channel options map
     *
     * @param url
     * @return
     */
    protected Map<ChannelOption, Object> getChildChannelOptionMap(URL url) {
        Map<ChannelOption, Object> channelOption = new HashMap<>();
        channelOption.put(
                ChannelOption.SO_KEEPALIVE, url.getParameter(Constants.KEEP_ALIVE_KEY, Constants.DEFAULT_KEEP_ALIVE));
        return channelOption;
    }

    /**
     * create channel options map
     *
     * @param url
     * @return
     */
    protected Map<ChannelOption, Object> getChannelOptionMap(URL url) {
        Map<ChannelOption, Object> options = new HashMap<>();

        options.put(ChannelOption.SO_REUSEADDR, Boolean.TRUE);
        options.put(ChannelOption.TCP_NODELAY, Boolean.TRUE);
        options.put(
                ChannelOption.SO_BACKLOG,
                url.getPositiveParameter(BACKLOG_KEY, org.apache.dubbo.remoting.Constants.DEFAULT_BACKLOG));
        options.put(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        return options;
    }

    /**
     * create channel handler
     *
     * @param url
     * @return
     */
    protected List<ChannelHandler> getChannelHandlers(URL url) {
        List<ChannelHandler> channelHandlers = new ArrayList<>();

        return channelHandlers;
    }

    @Override
    public void deploy(ServiceRestMetadata serviceRestMetadata, Invoker invoker) {
        serviceDeployer.deploy(serviceRestMetadata, invoker);
    }

    @Override
    public void undeploy(ServiceRestMetadata serviceRestMetadata) {
        serviceDeployer.undeploy(serviceRestMetadata);
    }

    private void registerExtension(URL url) {
        serviceDeployer.registerExtension(url);
    }
}
