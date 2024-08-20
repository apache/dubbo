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
import org.apache.dubbo.xds.resource.listener.Listener;

import java.util.Objects;

public class LdsUpdate implements ResourceUpdate {

    private HttpConnectionManager httpConnectionManager;
    private Listener listener;
    private io.envoyproxy.envoy.config.listener.v3.Listener rawListener;

    public LdsUpdate(HttpConnectionManager httpConnectionManager, Listener listener) {
        this.httpConnectionManager = httpConnectionManager;
        this.listener = listener;
    }

    public HttpConnectionManager getHttpConnectionManager() {
        return httpConnectionManager;
    }

    public void setHttpConnectionManager(HttpConnectionManager httpConnectionManager) {
        this.httpConnectionManager = httpConnectionManager;
    }

    public Listener getListener() {
        return listener;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public io.envoyproxy.envoy.config.listener.v3.Listener getRawListener() {
        return rawListener;
    }

    public void setRawListener(io.envoyproxy.envoy.config.listener.v3.Listener rawListener) {
        this.rawListener = rawListener;
    }

    @Override
    public String toString() {
        return "XdsListenerResourceLdsUpdate{" + "httpConnectionManager=" + httpConnectionManager + ", " + "listener="
                + listener + "}";
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
        return Objects.equals(httpConnectionManager, that.httpConnectionManager)
                && Objects.equals(listener, that.listener);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpConnectionManager, listener);
    }

    public static LdsUpdate forApiListener(HttpConnectionManager httpConnectionManager) {
        Assert.notNull(httpConnectionManager, "httpConnectionManager must not be null");
        return new LdsUpdate(httpConnectionManager, null);
    }

    public static LdsUpdate forTcpListener(Listener listener) {
        Assert.notNull(listener, "listener must not be null");
        return new LdsUpdate(null, listener);
    }
}
