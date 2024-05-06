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
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.remoting.transport.AbstractServer;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.EXT_PROTOCOL;

public abstract class AbstractPortUnificationServer extends AbstractServer {

    /**
     * extension name -> activate WireProtocol
     */
    private final Map<String, WireProtocol> protocols;

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
        ExtensionLoader<WireProtocol> loader = url.getOrDefaultFrameworkModel().getExtensionLoader(WireProtocol.class);
        Map<String, WireProtocol> protocols = loader.getActivateExtension(url, new String[0]).stream()
                .collect(Collectors.toConcurrentMap(loader::getExtensionName, Function.identity()));
        // load extra protocols
        String extraProtocols = url.getParameter(EXT_PROTOCOL);
        if (StringUtils.isNotEmpty(extraProtocols)) {
            Arrays.stream(extraProtocols.split(COMMA_SEPARATOR)).forEach(p -> {
                protocols.put(p, loader.getExtension(p));
            });
        }
        this.protocols = protocols;
    }

    public Map<String, WireProtocol> getProtocols() {
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

    public Map<String, URL> getSupportedUrls() {
        // this getter is just used by implementation of this class
        return supportedUrls;
    }

    public Map<String, ChannelHandler> getSupportedHandlers() {
        // this getter is just used by implementation of this class
        return supportedHandlers;
    }
}
