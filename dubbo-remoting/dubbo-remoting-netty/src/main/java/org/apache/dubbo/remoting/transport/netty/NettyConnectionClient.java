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
package org.apache.dubbo.remoting.transport.netty;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;

import org.jboss.netty.channel.Channel;

public class NettyConnectionClient extends AbstractConnectionClient {

    public NettyConnectionClient(URL url, ChannelHandler handler) throws RemotingException {
        super(url, handler);
    }

    @Override
    protected void initConnectionClient() {
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void createConnectingPromise() {
    }

    @Override
    public void addCloseListener(Runnable func) {
    }

    @Override
    public void onConnected(Object channel) {
    }

    @Override
    public void onGoaway(Object channel) {
    }

    @Override
    public void destroy() {
        close();
    }

    @Override
    public Object getChannel(Boolean generalizable) {
        return null;
    }

    @Override
    protected void doOpen() throws Throwable {
    }

    @Override
    protected void doClose() throws Throwable {
    }

    @Override
    protected void doConnect() throws Throwable {
    }

    @Override
    protected void doDisConnect() throws Throwable {
    }

    @Override
    protected org.apache.dubbo.remoting.Channel getChannel() {
        return null;
    }

    Channel getNettyChannel() {
        return null;
    }
}
