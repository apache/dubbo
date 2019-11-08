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
package org.apache.dubbo.rpc.protocol.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.RemotingServer;
import org.apache.dubbo.rpc.ProtocolServer;

public class DubboProtocolServer implements ProtocolServer {

    private RemotingServer server;
    private String address;

    public DubboProtocolServer(RemotingServer server) {
        this.server = server;
    }

    @Override
    public RemotingServer getRemotingServer() {
        return server;
    }

    @Override
    public String getAddress() {
        return StringUtils.isNotEmpty(address) ? address : server.getUrl().getAddress();
    }

    @Override
    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public URL getUrl() {
        return server.getUrl();
    }

    @Override
    public void reset(URL url) {
        server.reset(url);
    }

    @Override
    public void close() {
        server.close();
    }
}
