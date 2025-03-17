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

import org.apache.dubbo.common.Parameters;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.support.header.HeaderExchangeClient;

import java.net.InetSocketAddress;

public class CustomExchangeClient implements ExchangeClient {
    private final HeaderExchangeClient delegate;

    public CustomExchangeClient(HeaderExchangeClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return delegate.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return delegate.getRemoteAddress();
    }

    @Override
    public void send(Object message) throws RemotingException {
        delegate.send(message);
    }

    @Override
    public void send(Object message, boolean sent) throws RemotingException {
        delegate.send(message, sent);
    }

    @Override
    public URL getUrl() {
        return delegate.getUrl();
    }

    @Override
    public ChannelHandler getChannelHandler() {
        return delegate.getChannelHandler();
    }

    @Override
    public void reset(Parameters parameters) {
        delegate.reset(parameters);
    }
}
