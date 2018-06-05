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

package com.alibaba.dubbo.remoting.transport.ipc;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.Server;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerAdapter;
import io.netty.util.internal.SystemPropertyUtil;
import org.junit.Test;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * IpcTransporterTest
 * Based on Linux only.
 */
public class IpcTransporterTest {

    @Test
    public void shouldAbleToBindIpc() throws Exception {
        String os = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();
        if (!os.startsWith("linux")) {
            return;
        }
        int port = NetUtils.getAvailablePort();
        URL url = new URL("dubbo", "localhost", port,
                new String[]{Constants.BIND_PORT_KEY, String.valueOf(port)});
        //to not create too much child group
        url.addParameter(Constants.IO_THREADS_KEY, 1);

        Server server = new IpcTransporter().bind(url, new ChannelHandlerAdapter());

        assertThat(server.isBound(), is(true));
    }

    @Test
    public void shouldConnectToIpcServer() throws Exception {
        String os = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();
        System.getProperty("java.io.tmpdir");
        if (!os.startsWith("linux")) {
            return;
        }

        final CountDownLatch lock = new CountDownLatch(1);
        int port = NetUtils.getAvailablePort();
        URL url = new URL("dubbo", "localhost", port,
                new String[]{Constants.BIND_PORT_KEY, String.valueOf(port)});
        //to not create too much child group
        url.addParameter(Constants.IO_THREADS_KEY, 1);

        new IpcTransporter().bind(url, new ChannelHandlerAdapter() {

            @Override
            public void connected(Channel channel) throws RemotingException {
                lock.countDown();
            }
        });

        Thread.sleep(1000);

        new IpcTransporter().connect(url, new ChannelHandlerAdapter() {
            @Override
            public void sent(Channel channel, Object message) throws RemotingException {
                channel.send(message);
                channel.close();
            }
        });

        lock.await();
    }
}