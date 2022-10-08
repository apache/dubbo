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
package org.apache.dubbo.remoting.api.pu;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.remoting.transport.AbstractServer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractPortUnificationServer extends AbstractServer {
    private final List<WireProtocol> protocols;

    /*
    protocol name --> URL object
    wire protocol will get url object to config server pipeline for channel
     */
    private final Map<String, URL> supportedUrls = new ConcurrentHashMap<>();

    /*
    protocol name --> ChannelHandler object
    wire protocol will get handler to config server pipeline for channel
    (for triple protocol, it's a default handler that do nothing)
     */
    private final Map<String, ChannelHandler> supportedHandlers = new ConcurrentHashMap<>();

    public AbstractPortUnificationServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        this.protocols = url.getOrDefaultFrameworkModel().getExtensionLoader(WireProtocol.class).getActivateExtension(url, new String[0]);
    }

    public List<WireProtocol> getProtocols() {
        return protocols;
    }

    /*
    This method registers URL object and corresponding channel handler to pu server.
    In PuServerExchanger.bind, this method is called with ConcurrentHashMap.computeIfPresent to register messages to
    this supportedUrls and supportedHandlers
     */
    public void addSupportedProtocol(URL url, ChannelHandler handler) {
        this.supportedUrls.put(url.getProtocol(), url);
        this.supportedHandlers.put(url.getProtocol(), handler);
    }

    protected Map<String, URL> getSupportedUrls() {
        // this getter is just used by implementation of this class
        return supportedUrls;
    }

    public Map<String, ChannelHandler> getSupportedHandlers() {
        // this getter is just used by implementation of this class
        return supportedHandlers;
    }
}
