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
package org.apache.dubbo.rpc.protocol.tri.h3.negotiation;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.connection.AbstractConnectionClient;
import org.apache.dubbo.remoting.exchange.PortUnificationExchanger;
import org.apache.dubbo.remoting.transport.netty4.NettyHttp3ConnectionClient;
import org.apache.dubbo.rpc.protocol.tri.ExceptionUtils;

public class Helper {

    private Helper() {}

    public static AbstractConnectionClient createAutoSwitchClient(URL url, ChannelHandler handler) {
        return new AutoSwitchConnectionClient(url, PortUnificationExchanger.connect(url, handler));
    }

    public static AbstractConnectionClient createHttp3Client(URL url, ChannelHandler handler) {
        try {
            return new NettyHttp3ConnectionClient(url, handler);
        } catch (RemotingException e) {
            throw ExceptionUtils.wrap(e);
        }
    }
}
