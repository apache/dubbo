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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.p2p.Group;
import org.apache.dubbo.remoting.p2p.Peer;
import org.apache.dubbo.remoting.transport.ChannelHandlerAdapter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

public class FileNetworkerTest {

    @BeforeEach
    public void setUp(@TempDir Path folder) throws Exception {
        folder.toFile().createNewFile();
    }

    @AfterEach
    public void tearDown(@TempDir Path folder) {
        folder.getFileName().toAbsolutePath().toFile().delete();
    }

    @Test
    public void testJoin(@TempDir Path folder) throws RemotingException, InterruptedException {
        final String groupURL = "file:///" + folder.getFileName().toAbsolutePath();

        FileNetworker networker = new FileNetworker();
        Group group = networker.lookup(URL.valueOf(groupURL));

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Peer peer1 = group.join(URL.valueOf("dubbo://0.0.0.0:" + NetUtils.getAvailablePort()), new ChannelHandlerAdapter() {
            @Override
            public void received(Channel channel, Object message) {
                countDownLatch.countDown();
            }
        });
        Peer peer2 = group.join(URL.valueOf("dubbo://0.0.0.0:" + NetUtils.getAvailablePort()),
                mock(ChannelHandlerAdapter.class));

        while (true) {
            long count = countDownLatch.getCount();
            if (count > 0) {
                break;
            }
            for (Channel channel : peer1.getChannels()) {
                channel.send(0, false);
                channel.send("hello world!");
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }


        peer2.close();
        peer1.close();
    }
}