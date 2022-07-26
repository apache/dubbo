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
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.remoting.api.pu.DefaultPuHandler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PortUnificationServerTest {

    @Test
    public void testBind() throws RemotingException {
        int port = NetUtils.getAvailablePort();
        URL url = URL.valueOf("empty://127.0.0.1:" + port + "?foo=bar");

        // abstract endpoint need to get codec of url(which is in triple package)
        final NettyPortUnificationServer server = new NettyPortUnificationServer(url, new DefaultPuHandler());
        server.bind();
        Assertions.assertTrue(server.isBound());
    }
}
