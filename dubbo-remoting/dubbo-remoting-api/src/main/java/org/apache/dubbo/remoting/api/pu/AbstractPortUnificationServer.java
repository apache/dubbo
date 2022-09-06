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

    private final Map<String, URL> urlMapper = new ConcurrentHashMap<>();

    private final Map<String, ChannelHandler> handlerMapper = new ConcurrentHashMap<>();

    public AbstractPortUnificationServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        this.protocols = url.getOrDefaultFrameworkModel().getExtensionLoader(WireProtocol.class).getActivateExtension(url, new String[0]);
    }

    public List<WireProtocol> getProtocols() {
        return protocols;
    }

    public void addNewURL(URL url, ChannelHandler handler) {
        this.urlMapper.put(url.getProtocol(), url);
        this.handlerMapper.put(url.getProtocol(), handler);
    }

    protected Map<String, URL> getUrlMapper() {
        // this getter is just used by implementation of this class
        return urlMapper;
    }

    public Map<String, ChannelHandler> getHandlerMapper() {
        // this getter is just used by implementation of this class
        return handlerMapper;
    }
}
