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
package org.apache.dubbo.remoting;

import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;

/**
 * TelnetServer
 */
public class TelnetServer {

    public static void main(String[] args) throws Exception {
        Transporters.bind("telnet://0.0.0.0:23", new ChannelHandlerAdapter() {
            @Override
            public void connected(Channel channel) throws RemotingException {
                channel.send("telnet> ");
            }

            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                channel.send("Echo: " + message + "\r\n");
                channel.send("telnet> ");
            }
        });
        // Prevent JVM from exiting
        synchronized (TelnetServer.class) {
            while (true) {
                try {
                    TelnetServer.class.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
