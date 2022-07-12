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
import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.transport.AbstractServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// this class add WireProtocol and URL management ability
public abstract class AbstractPortUnificationServer extends AbstractServer {
    private static final Logger logger = LoggerFactory.getLogger(AbstractPortUnificationServer.class);

    public List<WireProtocol> getProtocols() {
        return protocols;
    }

    public List<URL> getUrls() {
        return urls;
    }

    public ConcurrentMap<WireProtocol, URL> getWireProtocolURLConcurrentMap() {
        return wireProtocolURLConcurrentMap;
    }

    private final List<WireProtocol> protocols;

    private final List<URL> urls;
    private final ConcurrentMap<WireProtocol, URL> wireProtocolURLConcurrentMap = new ConcurrentHashMap<>();

    public AbstractPortUnificationServer(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
        this.protocols = new ArrayList<>();
        this.urls = new ArrayList<>();

        this.urls.add(url);
        final WireProtocol wp = ExtensionLoader.getExtensionLoader(WireProtocol.class).getExtension(url.getProtocol());
        this.protocols.add(wp);
        this.wireProtocolURLConcurrentMap.put(wp, url);
    }

    //todo may need to check channel state
    @Override
    public void received(Channel ch, Object msg) throws RemotingException {

        logger.debug("trigger protocol detect process");
        if (msg instanceof ChannelBuffer) {
            ChannelBuffer in = (ChannelBuffer) msg;
            if (in.readableBytes() < 5) {
                return;
            }

            for (final WireProtocol protocol : protocols) {
                in.markReaderIndex();
                final ProtocolDetector.Result result = protocol.detector().detect(ch, in);
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
    }
}
