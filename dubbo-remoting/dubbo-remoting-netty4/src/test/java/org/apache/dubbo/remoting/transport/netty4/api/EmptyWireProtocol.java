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
package org.apache.dubbo.remoting.transport.netty4.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.api.ProtocolDetector;
import org.apache.dubbo.remoting.api.WireProtocol;
import org.apache.dubbo.remoting.api.pu.ChannelOperator;
import org.apache.dubbo.remoting.api.ssl.ContextOperator;

public class EmptyWireProtocol implements WireProtocol {
    @Override
    public ProtocolDetector detector() {
        return null;
    }

    @Override
    public void configServerProtocolHandler(URL url, ChannelOperator operator) {}

    @Override
    public void configClientPipeline(URL url, ChannelOperator operator, ContextOperator contextOperator) {}

    @Override
    public void close() {}
}
