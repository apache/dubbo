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
package org.apache.dubbo.remoting.transport.netty4;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.ChannelHandler;
import org.apache.dubbo.remoting.Client;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.remoting.api.pu.AbstractPortUnificationServer;
import org.apache.dubbo.remoting.api.pu.PortUnificationTransporter;

public class NettyPortUnificationTransporter implements PortUnificationTransporter {

    public static final String NAME = "netty";

    private static volatile AbstractPortUnificationServer server = null ;

    public static RemotingServer getInstance(URL url, ChannelHandler handler) throws RemotingException {
        if ( server == null ) {
            synchronized ( RemotingServer.class ){
                if ( server == null ) {
                    server = new NettyPortUnificationServer(url, handler) ;
                } else {
                    //todo reset or other operation
                }
            }
        }

        return server ;
    }

    @Override
    public RemotingServer bind(URL url, ChannelHandler handler) throws RemotingException {
        return getInstance(url, handler);
    }

    @Override
    public Client connect(URL url, ChannelHandler handler) throws RemotingException {
        return null;
    }
}
