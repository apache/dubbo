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
package com.alibaba.dubbo.remoting.p2p;

import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerAdapter;

import java.util.Collection;

/**
 * PeerMain
 *
 * @author william.liangf
 */
public class PeerMain {

    public static void main(String[] args) throws Throwable {
        String groupURL = "multicast://224.5.6.7:9911"; // 组地址，支持multicast和file两种组 ，可扩展
        final String peerURL = "dubbo://0.0.0.0:" + (((int) (Math.random() * 10000)) + 20000); // 用于交叉组网的本机服务器地址

        // 加入组，并获取对等引用
        Peer peer = Networkers.join(groupURL, peerURL, new ChannelHandlerAdapter() {
            @Override
            public void received(Channel channel, Object message) throws RemotingException {
                System.out.println("Received: " + message + " in " + peerURL);
            }
        });

        // 向网络中存在的其它对等体发送消息
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            Collection<Channel> channels = peer.getChannels(); // 获取与其它所有对等体的通道，此列表动态变化
            if (channels != null && channels.size() > 0) {
                for (Channel channel : channels) {
                    channel.send("(" + i + ") " + peerURL); // 向指定对等体发送消息
                }
            }
            Thread.sleep(1000);
        }

        // 离开网络
        peer.leave();
    }

}