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
package org.apache.dubbo.rpc.protocol.tri.h3;

import org.apache.dubbo.remoting.api.DatagramProtocolDetector;
import org.apache.dubbo.remoting.buffer.ChannelBuffer;
import org.apache.dubbo.rpc.protocol.tri.h12.TripleProtocolDetector.HttpVersion;

import io.netty.channel.socket.DatagramPacket;

import static org.apache.dubbo.rpc.protocol.tri.h12.TripleProtocolDetector.HTTP_VERSION;

public class TripleHttp3ProtocolDetector implements DatagramProtocolDetector {
    @Override
    public Result detect(DatagramPacket in) {
        Result recognized = Result.recognized();
        recognized.setAttribute(HTTP_VERSION, HttpVersion.HTTP3.getVersion());
        return recognized;
    }

    @Override
    public Result detect(ChannelBuffer in) {
        return Result.unrecognized();
    }
}
