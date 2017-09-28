/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.remoting;

import com.alibaba.dubbo.remoting.transport.ChannelHandlerAdapter;

/**
 * TelnetServer
 *
 * @author william.liangf
 */
public class TelnetServer {

    public static void main(String[] args) throws Exception {
        Transporters.bind("telnet://0.0.0.0:23", new ChannelHandlerAdapter() {
            public void connected(Channel channel) throws RemotingException {
                channel.send("telnet> ");
            }

            public void received(Channel channel, Object message) throws RemotingException {
                channel.send("Echo: " + message + "\r\n");
                channel.send("telnet> ");
            }
        });
        // 阻止JVM退出
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