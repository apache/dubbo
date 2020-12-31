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

package com.alibaba.dubbo.remoting;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.remoting.Constants;
import org.apache.dubbo.remoting.RemotingServer;

import com.alibaba.dubbo.common.URL;

@Deprecated
public interface Transporter extends org.apache.dubbo.remoting.Transporter {

    @Adaptive({Constants.SERVER_KEY, Constants.TRANSPORTER_KEY})
    Server bind(URL url, ChannelHandler handler) throws RemotingException;

    @Override
    default RemotingServer bind(org.apache.dubbo.common.URL url, org.apache.dubbo.remoting.ChannelHandler handler)
            throws org.apache.dubbo.remoting.RemotingException {
        return bind(new URL(url), new ChannelHandler() {
            @Override
            public void connected(Channel channel) throws RemotingException {
                try {
                    handler.connected(channel);
                } catch (org.apache.dubbo.remoting.RemotingException e) {
                    throw new RemotingException(e);
                }
            }

            @Override
            public void disconnected(Channel channel) throws RemotingException {
                try {
                    handler.disconnected(channel);
                } catch (org.apache.dubbo.remoting.RemotingException e) {
                    throw new RemotingException(e);
                }
            }

            @Override
            public void sent(Channel channel, Object message) throws RemotingException {
                try {
                    handler.sent(channel, message);
                } catch (org.apache.dubbo.remoting.RemotingException e) {
                    throw new RemotingException(e);
                }
            }

            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                try {
                    handler.received(channel, message);
                } catch (org.apache.dubbo.remoting.RemotingException e) {
                    throw new RemotingException(e);
                }
            }

            @Override
            public void caught(Channel channel, Throwable exception) throws RemotingException {
                try {
                    handler.caught(channel, exception);
                } catch (org.apache.dubbo.remoting.RemotingException e) {
                    throw new RemotingException(e);
                }
            }
        });
    }
}
