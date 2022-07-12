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
import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.transport.AbstractServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public ConcurrentMap<URL, ChannelHandler> getUrlChannelHandlerConcurrentMap() {
        return urlChannelHandlerConcurrentMap;
    }
    private final List<NewWireProtocol> protocols;

    private final List<URL> urls;
    private final ConcurrentMap<NewWireProtocol, URL> wireProtocolURLConcurrentMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<URL, ChannelHandler> urlChannelHandlerConcurrentMap = new ConcurrentHashMap<>();

    public AbstractPortUnificationServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        this.protocols = new ArrayList<>();
        this.urls = new ArrayList<>();

        this.urls.add(url);
        final NewWireProtocol wp = ExtensionLoader.getExtensionLoader(NewWireProtocol.class).getExtension(url.getProtocol());
        this.protocols.add(wp);
        this.wireProtocolURLConcurrentMap.put(wp, url);
        this.urlChannelHandlerConcurrentMap.put(url, handler);
    }


    public void AddNewUrl(URL url, ChannelHandler handler) {
        this.urls.add(url);
        final NewWireProtocol wp = ExtensionLoader.getExtensionLoader(NewWireProtocol.class).getExtension(url.getProtocol());

        this.wireProtocolURLConcurrentMap.put(wp, url);
        this.protocols.add(wp);
        this.urlChannelHandlerConcurrentMap.put(url, handler);
    }

    @Override
    public void connected(Channel ch) throws RemotingException{
        // get url state of channel url to
        // decide whether call super class method
        String state = ch.getUrl().getParameter(Constants.PU_STATE, "");
        if (!state.equals("")) {
            super.connected(ch);
        }
    }
    @Override
    public void disconnected(Channel ch) throws RemotingException{
        String state = ch.getUrl().getParameter(Constants.PU_STATE, "");
        if (!state.equals("")) {
            super.disconnected(ch);
        }
    }

    @Override
    public void received(Channel ch, Object msg) throws RemotingException {
        String state = ch.getUrl().getParameter(Constants.PU_STATE, "");
        if(state.equals("")) {
            logger.debug("trigger protocol detect process");
            if (msg instanceof ChannelBuffer) {
                ChannelBuffer in = (ChannelBuffer) msg;
                if (in.readableBytes() < 5) {
                    return;
                }

                for (final NewWireProtocol protocol : protocols) {
                    in.markReaderIndex();
                    final NewProtocolDetector.Result result = protocol.detector().detect(ch, in);
                    in.resetReaderIndex();
                    switch (result) {
                        case UNRECOGNIZED:
                            continue;
                        case RECOGNIZED:
                            URL local_url = wireProtocolURLConcurrentMap.get(protocol);
                            //!todo here may need pass new handler of this URL to configServerPipeline
                            // and update url of channel to add more
                            protocol.configServerPipeline(local_url, ch);
                            // trigger connected event
                            // this event won't be triggered by nio framework,
                            // but this event is needed by protocols(such as Dubbo protocol)
                            this.connected(ch);
                            return;
                        case NEED_MORE_DATA:
                            return;
                        default:
                            return;
                    }
                }
                byte[] preface = new byte[in.readableBytes()];
                in.readBytes(preface);

                logger.error(String.format("Can not recognize protocol from downstream=%s . "
                        + "preface=%s protocols=%s", ch.getRemoteAddress(),
                    Bytes.bytes2hex(preface)));

                // Unknown protocol; discard everything and close the connection.
                in.clear();
                ch.close();
            }
        }else {
            super.received(ch, msg);
        }
    }

    @Override
    public void caught(Channel ch, Throwable ex) throws RemotingException {
        String state = ch.getUrl().getParameter(Constants.PU_STATE, "");
        if (!state.equals("")) {
            super.caught(ch, ex);
        }
    }

    @Override
    public void sent(Channel ch, Object msg) throws RemotingException {
        String state = ch.getUrl().getParameter(Constants.PU_STATE, "");
        if (!state.equals("")) {
            super.sent(ch, msg);
        }
    }
}
