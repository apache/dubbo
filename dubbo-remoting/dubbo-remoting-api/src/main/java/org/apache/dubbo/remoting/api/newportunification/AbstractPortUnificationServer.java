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
package org.apache.dubbo.remoting.api.newportunification;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.transport.AbstractServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// this class add WireProtocol and URL management ability
public abstract class AbstractPortUnificationServer extends AbstractServer {
    private static final Logger logger = LoggerFactory.getLogger(AbstractPortUnificationServer.class);

    public List<NewWireProtocol> getProtocols() {
        return protocols;
    }

    public List<URL> getUrls() {
        return urls;
    }

    public ConcurrentMap<NewWireProtocol, URL> getWireProtocolURLConcurrentMap() {
        return wireProtocolURLConcurrentMap;
    }

    private final List<NewWireProtocol> protocols;

    private final List<URL> urls;
    private final ConcurrentMap<NewWireProtocol, URL> wireProtocolURLConcurrentMap = new ConcurrentHashMap<>();

    public AbstractPortUnificationServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        this.protocols = new ArrayList<>();
        this.urls = new ArrayList<>();

        this.urls.add(url);
        final NewWireProtocol wp = ExtensionLoader.getExtensionLoader(NewWireProtocol.class).getExtension(url.getProtocol());
        this.protocols.add(wp);
        this.wireProtocolURLConcurrentMap.put(wp, url);
    }


    public void AddNewUrl(URL url) {
        this.urls.add(url);
        final NewWireProtocol wp = ExtensionLoader.getExtensionLoader(NewWireProtocol.class).getExtension(url.getProtocol());

        this.wireProtocolURLConcurrentMap.put(wp, url);
        this.protocols.add(wp);
    }
}
