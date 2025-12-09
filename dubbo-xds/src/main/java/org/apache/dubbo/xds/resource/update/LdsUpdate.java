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
package org.apache.dubbo.xds.resource.update;

import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.xds.resource.listener.HttpConnectionManager;

import java.util.Objects;

import io.envoyproxy.envoy.config.listener.v3.Listener;

public class LdsUpdate implements ResourceUpdate {

    private HttpConnectionManager httpConnectionManager;
    private org.apache.dubbo.xds.resource.listener.Listener listener;
    private Listener rawListener;
    private int port = -1;

    public LdsUpdate(
            HttpConnectionManager httpConnectionManager, org.apache.dubbo.xds.resource.listener.Listener listener) {
        this.httpConnectionManager = httpConnectionManager;
        this.listener = listener;
    }

    public LdsUpdate(
            HttpConnectionManager httpConnectionManager,
            org.apache.dubbo.xds.resource.listener.Listener listener,
            int port) {
        this.httpConnectionManager = httpConnectionManager;
        this.listener = listener;
        this.port = port;
    }

    public HttpConnectionManager getHttpConnectionManager() {
        return httpConnectionManager;
    }

    public void setHttpConnectionManager(HttpConnectionManager httpConnectionManager) {
        this.httpConnectionManager = httpConnectionManager;
    }

    public org.apache.dubbo.xds.resource.listener.Listener getListener() {
        return listener;
    }

    public void setListener(org.apache.dubbo.xds.resource.listener.Listener listener) {
        this.listener = listener;
    }

    public io.envoyproxy.envoy.config.listener.v3.Listener getRawListener() {
        return rawListener;
    }

    public void setRawListener(io.envoyproxy.envoy.config.listener.v3.Listener rawListener) {
        this.rawListener = rawListener;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isContainPort(int port) {
        return this.port == port;
    }

    @Override
    public String toString() {
        return "XdsListenerResourceLdsUpdate{" + "httpConnectionManager=" + httpConnectionManager + ", " + "listener="
                + listener + ", port=" + port + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LdsUpdate)) {
            return false;
        }
        LdsUpdate that = (LdsUpdate) o;
        return port == that.port
                && Objects.equals(httpConnectionManager, that.httpConnectionManager)
                && Objects.equals(listener, that.listener);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpConnectionManager, listener, port);
    }

    public static LdsUpdate forApiListener(HttpConnectionManager httpConnectionManager) {
        Assert.notNull(httpConnectionManager, "httpConnectionManager must not be null");
        return new LdsUpdate(httpConnectionManager, null);
    }

    public static LdsUpdate forApiListener(HttpConnectionManager httpConnectionManager, int port) {
        Assert.notNull(httpConnectionManager, "httpConnectionManager must not be null");
        return new LdsUpdate(httpConnectionManager, null, port);
    }

    public static LdsUpdate forTcpListener(org.apache.dubbo.xds.resource.listener.Listener listener) {
        Assert.notNull(listener, "listener must not be null");
        return new LdsUpdate(null, listener);
    }

    public static LdsUpdate forTcpListener(org.apache.dubbo.xds.resource.listener.Listener listener, int port) {
        Assert.notNull(listener, "listener must not be null");
        return new LdsUpdate(null, listener, port);
    }
}
