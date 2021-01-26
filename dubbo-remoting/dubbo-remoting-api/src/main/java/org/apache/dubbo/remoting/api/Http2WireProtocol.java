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
package org.apache.dubbo.remoting.api;

import io.netty.handler.codec.http2.Http2FrameLogger;

import static io.netty.handler.logging.LogLevel.DEBUG;

public abstract class Http2WireProtocol implements WireProtocol {
    public static final Http2FrameLogger CLIENT_LOGGER = new Http2FrameLogger(DEBUG, "H2_CLIENT");
    public static final Http2FrameLogger SERVER_LOGGER = new Http2FrameLogger(DEBUG, "H2_SERVER");
    private final ProtocolDetector detector = new Http2ProtocolDetector();

    @Override
    public ProtocolDetector detector() {
        return detector;
    }

    @Override
    public void close() {
    }
}
