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
import org.apache.dubbo.common.io.Bytes;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.remoting.transport.ChannelHandlerDelegate;

import java.util.List;
import java.util.concurrent.ConcurrentMap;


public class PortUnificationServerHandlerDelegate implements ChannelHandlerDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        PortUnificationServerHandlerDelegate.class);

    // inner handler of this wrapper
    private final ConcurrentMap<URL, ChannelHandler> urlChannelHandlerConcurrentMap;

    public final List<NewWireProtocol> protocols;
    public final ConcurrentMap<NewWireProtocol, URL> wireProtocolURLConcurrentMap;

    public PortUnificationServerHandlerDelegate(List<NewWireProtocol> protocols,
                                                ConcurrentMap<NewWireProtocol, URL> wireProtocolURLConcurrentMap,
                                                ConcurrentMap<URL, ChannelHandler> urlChannelHandlerConcurrentMap) {
        this.protocols = protocols;
        this.urlChannelHandlerConcurrentMap = urlChannelHandlerConcurrentMap;
        this.wireProtocolURLConcurrentMap = wireProtocolURLConcurrentMap;
    }

    @Override
    public void connected(Channel channel) throws RemotingException {
        if(channel.getUrl() != null) {
           this.urlChannelHandlerConcurrentMap.get(channel.getUrl())
               .connected(channel);
        }
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        if(channel.getUrl() != null) {
            this.urlChannelHandlerConcurrentMap.get(channel.getUrl())
                .disconnected(channel);
        }
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        if(channel.getUrl() != null) {
            this.urlChannelHandlerConcurrentMap.get(channel.getUrl())
                .sent(channel, message);
        }
    }

    //  received: use wire protocol to recognize channel's protocol
    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        // message in this method should be a ChannelBuffer
        if(channel.getUrl() == null) {
            LOGGER.debug("trigger handler delegate for wire protocol ");
            if (message instanceof ChannelBuffer) {
                ChannelBuffer in = (ChannelBuffer) message;
                if (in.readableBytes() < 5) {
                    return;
                }

                for (final NewWireProtocol protocol : protocols) {
                    in.markReaderIndex();
                    final NewProtocolDetector.Result result = protocol.detector().detect(channel, in);
                    in.resetReaderIndex();
                    switch (result) {
                        case UNRECOGNIZED:
                            continue;
                        case RECOGNIZED:
                            URL local_url = wireProtocolURLConcurrentMap.get(protocol);
                            //!todo here may need pass new handler of this URL to configServerPipeline
                            protocol.configServerPipeline(local_url, channel);
                            // trigger connected event
                            // this event won't be triggered by nio framework,
                            // but this event is needed by protocols(such as Dubbo protocol)
                            this.connected(channel);
                            return;
                        case NEED_MORE_DATA:
                            return;
                        default:
                            return;
                    }
                }
                byte[] preface = new byte[in.readableBytes()];
                in.readBytes(preface);

                LOGGER.error(String.format("Can not recognize protocol from downstream=%s . "
                        + "preface=%s protocols=%s", channel.getRemoteAddress(),
                    Bytes.bytes2hex(preface)));

                // Unknown protocol; discard everything and close the connection.
                in.clear();
                channel.close();
            }
        }else {
            this.urlChannelHandlerConcurrentMap.get(channel.getUrl())
                .received(channel, message);
        }
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        LOGGER.error("unexpected exception from downstream before detected.", exception);
    }

    @Override
    public ChannelHandler getHandler() {
        return this;
    }
}
