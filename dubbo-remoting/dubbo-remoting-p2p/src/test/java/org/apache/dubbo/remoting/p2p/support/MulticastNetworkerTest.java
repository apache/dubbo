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
package org.apache.dubbo.remoting.p2p.support;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.p2p.Group;
import org.apache.dubbo.remoting.p2p.Networkers;
import org.apache.dubbo.remoting.p2p.Peer;
import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MulticastNetworkerTest {

    @Test
    public void testJoin() throws RemotingException, InterruptedException {
        final String groupURL = "multicast://224.5.6.7:1234";
        final String peerURL = "dubbo://0.0.0.0:" + NetUtils.getAvailablePort();

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Peer peer1 = Networkers.join(groupURL, peerURL, new ChannelHandlerAdapter() {
            @Override
            public void received(Channel channel, Object message) {
                countDownLatch.countDown();
            }
        });
        Peer peer2 = Networkers.join(groupURL, "dubbo://0.0.0.0:" + NetUtils.getAvailablePort(),
                mock(ChannelHandlerAdapter.class));

        while (true) {
            long count = countDownLatch.getCount();
            if (count > 0) {
                break;
            }
            for (Channel channel : peer1.getChannels()) {
                channel.send("hello world!");
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }

        Group lookup = Networkers.lookup(groupURL);
        assertThat(lookup, not(nullValue()));

        peer2.close();
        peer1.close();
    }
}