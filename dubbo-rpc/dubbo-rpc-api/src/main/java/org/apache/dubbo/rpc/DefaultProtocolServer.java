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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.RemotingServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link ProtocolServer}.
 * <p>
 * This class wraps a {@link RemotingServer} and provides a simple implementation
 * of the {@link ProtocolServer} interface. It is used by protocols that don't need
 * special protocol server implementations, such as Triple protocol when using
 * port unification.
 * </p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li>Wraps a {@link RemotingServer} instance</li>
 *   <li>Supports custom address override</li>
 *   <li>Provides thread-safe attribute storage</li>
 *   <li>Delegates all operations to the underlying remoting server</li>
 * </ul>
 *
 * @see ProtocolServer
 * @see RemotingServer
 * @since 3.3
 */
public class DefaultProtocolServer implements ProtocolServer {

    /**
     * The underlying remoting server.
     */
    private final RemotingServer server;

    /**
     * Custom address override. If null, the server's URL address is used.
     */
    private String address;

    /**
     * Thread-safe storage for custom attributes.
     */
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    /**
     * Create a new DefaultProtocolServer wrapping the given remoting server.
     *
     * @param server the underlying remoting server, must not be null
     */
    public DefaultProtocolServer(RemotingServer server) {
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

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
