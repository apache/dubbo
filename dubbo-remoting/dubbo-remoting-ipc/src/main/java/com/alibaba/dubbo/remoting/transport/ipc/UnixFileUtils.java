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

import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.Socket;

import java.io.File;
import java.io.IOException;

final class UnixFileUtils {

    public static DomainSocketAddress newDomainSocketAddress() {
        try {
            File file;
            do {
                file = File.createTempFile("DUBBO-IPC", "UDS");
                if (!file.delete()) {
                    throw new IOException("failed to delete: " + file);
                }
            } while (file.getAbsolutePath().length() > Socket.UDS_SUN_PATH_SIZE);
            return new DomainSocketAddress(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private UnixFileUtils() {
    }
}