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
package org.apache.dubbo.remoting.handler;

import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;

import java.util.Collections;
import java.util.Set;

public class MockedChannelHandler implements ChannelHandler {
    //    ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<String, Channel>();
    ConcurrentHashSet<Channel> channels = new ConcurrentHashSet<Channel>();

    @Override
    public void connected(Channel channel) throws RemotingException {
        channels.add(channel);
    }

    @Override
    public void disconnected(Channel channel) throws RemotingException {
        channels.remove(channel);
    }

    @Override
    public void sent(Channel channel, Object message) throws RemotingException {
        channel.send(message);
    }

    @Override
    public void received(Channel channel, Object message) throws RemotingException {
        //echo 
        channel.send(message);
    }

    @Override
    public void caught(Channel channel, Throwable exception) throws RemotingException {
        throw new RemotingException(channel, exception);

    }

    public Set<Channel> getChannels() {
        return Collections.unmodifiableSet(channels);
    }
}
