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
package org.apache.dubbo.remoting.p2p.exchange.support;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.exchange.ExchangeChannel;
import org.apache.dubbo.remoting.exchange.ExchangeHandler;
import org.apache.dubbo.remoting.exchange.support.ExchangeHandlerAdapter;
import org.apache.dubbo.remoting.p2p.Group;
import org.apache.dubbo.remoting.p2p.Networkers;
import org.apache.dubbo.remoting.p2p.Peer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MulticastExchangeNetworkerTest {
    @Test
    public void testJoin() throws RemotingException, InterruptedException {
        final String groupURL = "multicast://224.5.6.7:1234";

        MulticastExchangeNetworker multicastExchangeNetworker = new MulticastExchangeNetworker();

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Peer peer1 = multicastExchangeNetworker.lookup(URL.valueOf(groupURL))
                .join(URL.valueOf("dubbo://0.0.0.0:" + NetUtils.getAvailablePort()), new ExchangeHandlerAdapter() {
                    @Override
                    public CompletableFuture<Object> reply(ExchangeChannel channel, Object msg) throws RemotingException {
                        countDownLatch.countDown();
                        return super.reply(channel, msg);
                    }
                });
        Peer peer2 = multicastExchangeNetworker.lookup(URL.valueOf(groupURL))
                .join(URL.valueOf("dubbo://0.0.0.0:" + NetUtils.getAvailablePort()), mock(ExchangeHandler.class));

        while (true) {
            for (Channel channel : peer1.getChannels()) {
                channel.send("hello multicast exchange network!");
            }
            TimeUnit.MILLISECONDS.sleep(50);

            long count = countDownLatch.getCount();
            if (count > 0) {
                break;
            }
        }

        Group lookup = Networkers.lookup(groupURL);
        assertThat(lookup, not(nullValue()));

        assertThat(peer1, instanceOf(ExchangeServerPeer.class));

        peer1.close();
        peer2.close();
    }
}