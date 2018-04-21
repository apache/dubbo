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
package com.alibaba.dubbo.rpc.protocol.rest;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;

import io.netty.channel.ChannelOption;
import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;

import java.util.HashMap;
import java.util.Map;

/**
 * Netty server can't support @Context injection of servlet objects since it's not a servlet container
 *
 */
public class NettyServer extends BaseRestServer {

    private final NettyJaxrsServer server = new NettyJaxrsServer();

    @Override
    protected void doStart(URL url) {
        String bindIp = url.getParameter(Constants.BIND_IP_KEY, url.getHost());
        if (!url.isAnyHost() && NetUtils.isValidLocalHost(bindIp)) {
            server.setHostname(bindIp);
        }
        server.setPort(url.getParameter(Constants.BIND_PORT_KEY, url.getPort()));
        Map<ChannelOption, Object> channelOption = new HashMap<ChannelOption, Object>();
        channelOption.put(ChannelOption.SO_KEEPALIVE, url.getParameter(Constants.KEEP_ALIVE_KEY, Constants.DEFAULT_KEEP_ALIVE));
        server.setChildChannelOptions(channelOption);
        server.setExecutorThreadCount(url.getParameter(Constants.THREADS_KEY, Constants.DEFAULT_THREADS));
        server.setIoWorkerCount(url.getParameter(Constants.IO_THREADS_KEY, Constants.DEFAULT_IO_THREADS));
        server.setMaxRequestSize(url.getParameter(Constants.PAYLOAD_KEY, Constants.DEFAULT_PAYLOAD));
        server.start();
    }

    @Override
    public void stop() {
        server.stop();
    }

    @Override
    protected ResteasyDeployment getDeployment() {
        return server.getDeployment();
    }
}
