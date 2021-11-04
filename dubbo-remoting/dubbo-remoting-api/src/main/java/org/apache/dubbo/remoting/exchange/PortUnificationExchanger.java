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
import org.apache.dubbo.remoting.api.PortUnificationServer;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PortUnificationExchanger {

    private static final Logger log = LoggerFactory.getLogger(PortUnificationExchanger.class);
    private static final ConcurrentMap<String, PortUnificationServer> servers = new ConcurrentHashMap<>();

    public static void bind(URL url) {
        servers.computeIfAbsent(url.getAddress(), addr -> {
            final PortUnificationServer server = new PortUnificationServer(url);
            server.bind();
            return server;
        });
    }

    public static void close() {
        final ArrayList<PortUnificationServer> toClose = new ArrayList<>(servers.values());
        servers.clear();
        for (PortUnificationServer server : toClose) {
            try {
                server.close();
            } catch (Throwable throwable) {
                log.error("Close all port unification server failed", throwable);
            }
        }
    }

    // for test
    public static ConcurrentMap<String, PortUnificationServer> getServers() {
        return servers;
    }
}
