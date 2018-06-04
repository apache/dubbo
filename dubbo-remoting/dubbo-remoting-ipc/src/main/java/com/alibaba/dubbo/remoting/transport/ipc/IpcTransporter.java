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

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.*;
import io.netty.util.internal.SystemPropertyUtil;

import java.util.Locale;

/**
 * IpcTransporter
 * Based on Netty4 and only Linux.
 */
public class IpcTransporter implements Transporter {

    public static final String NAME = "ipc";

    @Override
    public Server bind(URL url, ChannelHandler handler) throws RemotingException {
        checkOS();
        return new IpcServer(url, handler);
    }

    @Override
    public Client connect(URL url, ChannelHandler handler) throws RemotingException {
        checkOS();
        return new IpcClient(url, handler);
    }

    /**
     * Ipc transport is only based on Linux.
     */
    private void checkOS() {
        String name = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();
        if (!name.startsWith("linux")) {
            throw new IllegalStateException("Only supported on Linux");
        }
    }
}
