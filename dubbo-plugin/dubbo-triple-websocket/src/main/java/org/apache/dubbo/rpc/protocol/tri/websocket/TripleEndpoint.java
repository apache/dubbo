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
package org.apache.dubbo.rpc.protocol.tri.websocket;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.io.StreamUtils;
import org.apache.dubbo.config.context.ConfigManager;
import org.apache.dubbo.config.nested.TripleConfig;
import org.apache.dubbo.remoting.http12.HttpHeaderNames;
import org.apache.dubbo.remoting.http12.HttpHeaders;
import org.apache.dubbo.remoting.http12.HttpMethods;
import org.apache.dubbo.remoting.http12.HttpStatus;
import org.apache.dubbo.remoting.http12.h2.Http2Header;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessage;
import org.apache.dubbo.remoting.http12.h2.Http2InputMessageFrame;
import org.apache.dubbo.remoting.http12.h2.Http2MetadataFrame;
import org.apache.dubbo.remoting.http12.message.DefaultHttpHeaders;
import org.apache.dubbo.remoting.websocket.WebSocketTransportListener;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.tri.ServletExchanger;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import static org.apache.dubbo.rpc.protocol.tri.websocket.WebSocketConstants.TRIPLE_WEBSOCKET_LISTENER;

public class TripleEndpoint extends Endpoint {

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        String path = session.getRequestURI().getPath();
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.set(HttpHeaderNames.PATH.getName(), path);
        httpHeaders.set(HttpHeaderNames.METHOD.getName(), HttpMethods.POST.name());
        Http2Header http2Header = new Http2MetadataFrame(httpHeaders);

        URL url = ServletExchanger.getUrl();
        TripleConfig tripleConfig = ConfigManager.getProtocolOrDefault(url).getTripleOrDefault();

        WebSocketStreamChannel webSocketStreamChannel = new WebSocketStreamChannel(session, tripleConfig);
        WebSocketTransportListener webSocketTransportListener =
                DefaultWebSocketServerTransportListenerFactory.INSTANCE.newInstance(
                        webSocketStreamChannel, url, FrameworkModel.defaultModel());
        webSocketTransportListener.onMetadata(http2Header);
        session.addMessageHandler(new TripleTextMessageHandler(webSocketTransportListener));
        session.addMessageHandler(new TripleBinaryMessageHandler(webSocketTransportListener));
        session.getUserProperties().put(TRIPLE_WEBSOCKET_LISTENER, webSocketTransportListener);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        super.onClose(session, closeReason);
        WebSocketTransportListener webSocketTransportListener =
                (WebSocketTransportListener) session.getUserProperties().get(TRIPLE_WEBSOCKET_LISTENER);
        if (webSocketTransportListener == null) {
            return;
        }
        if (closeReason.getCloseCode().getCode() == CloseCodes.NORMAL_CLOSURE.getCode()) {
            Http2InputMessage http2InputMessage = new Http2InputMessageFrame(StreamUtils.EMPTY, true);
            webSocketTransportListener.onData(http2InputMessage);
            return;
        }
        webSocketTransportListener.cancelByRemote(closeReason.getCloseCode().getCode());
    }

    @Override
    public void onError(Session session, Throwable thr) {
        super.onError(session, thr);
        WebSocketTransportListener webSocketTransportListener =
                (WebSocketTransportListener) session.getUserProperties().get(TRIPLE_WEBSOCKET_LISTENER);
        if (webSocketTransportListener == null) {
            return;
        }
        webSocketTransportListener.cancelByRemote(HttpStatus.INTERNAL_SERVER_ERROR.getCode());
    }
}
