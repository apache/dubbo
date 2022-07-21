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
package org.apache.dubbo.remoting.exchange;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.api.pu.AbstractPortUnificationServer;
import org.apache.dubbo.remoting.api.pu.PortUnificationTransporter;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PortUnificationExchanger {

    private static final Logger log = LoggerFactory.getLogger(PortUnificationExchanger.class);
    private static final ConcurrentMap<String, RemotingServer> servers = new ConcurrentHashMap<>();

    public static void bind(URL url, ChannelHandler handler) {
        servers.computeIfAbsent(url.getAddress(), addr -> {
            final AbstractPortUnificationServer server;
            try {
                server = createServer(url, handler);
            } catch (RemotingException e) {
                throw new RuntimeException(e);
            }
            // server.bind();
            return server;
        });
    }

    public static void close() {
        final ArrayList<RemotingServer> toClose = new ArrayList<>(servers.values());
        servers.clear();
        for (RemotingServer server : toClose) {
            try {
                server.close();
            } catch (Throwable throwable) {
                log.error("Close all port unification server failed", throwable);
            }
        }
    }

    // for test
    public static ConcurrentMap<String, RemotingServer> getServers() {
        return servers;
    }

    public static PortUnificationTransporter getTransporter(URL url) {
        return url.getOrDefaultFrameworkModel().getExtensionLoader(PortUnificationTransporter.class)
            .getAdaptiveExtension();
    }

    public static AbstractPortUnificationServer createServer(URL url, ChannelHandler handler) throws RemotingException {
        // if url don't config server key and transporter key,
        // add a default transporter key to load pu server transporter
        url = url.addParameterIfAbsent(Constants.TRANSPORTER_KEY, Constants.DEFAULT_TRANSPORTER);
        return getTransporter(url).bind(url, handler);
    }
}
