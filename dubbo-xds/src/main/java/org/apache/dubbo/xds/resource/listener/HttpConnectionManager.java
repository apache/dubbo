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
package org.apache.dubbo.xds.resource.listener;

import org.apache.dubbo.common.lang.Nullable;
import org.apache.dubbo.common.utils.Assert;
import org.apache.dubbo.xds.resource.filter.NamedFilterConfig;
import org.apache.dubbo.xds.resource.route.VirtualHost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class HttpConnectionManager {

    private long httpMaxStreamDurationNano;
    private String rdsName;
    private List<VirtualHost> virtualHosts;
    private List<NamedFilterConfig> httpFilterConfigs;

    public HttpConnectionManager(
            long httpMaxStreamDurationNano,
            String rdsName,
            List<VirtualHost> virtualHosts,
            List<NamedFilterConfig> httpFilterConfigs) {
        this.httpMaxStreamDurationNano = httpMaxStreamDurationNano;
        this.rdsName = rdsName;
        this.virtualHosts = virtualHosts != null ? Collections.unmodifiableList(new ArrayList<>(virtualHosts)) : null;
        this.httpFilterConfigs =
                httpFilterConfigs != null ? Collections.unmodifiableList(new ArrayList<>(httpFilterConfigs)) : null;
    }

    public long getHttpMaxStreamDurationNano() {
        return httpMaxStreamDurationNano;
    }

    public void setHttpMaxStreamDurationNano(long httpMaxStreamDurationNano) {
        this.httpMaxStreamDurationNano = httpMaxStreamDurationNano;
    }

    public String getRdsName() {
        return rdsName;
    }

    public void setRdsName(String rdsName) {
        this.rdsName = rdsName;
    }

    public List<VirtualHost> getVirtualHosts() {
        return virtualHosts;
    }

    public void setVirtualHosts(List<VirtualHost> virtualHosts) {
        this.virtualHosts = virtualHosts != null ? new ArrayList<>(virtualHosts) : null;
    }

    public List<NamedFilterConfig> getHttpFilterConfigs() {
        return httpFilterConfigs;
    }

    public void setHttpFilterConfigs(List<NamedFilterConfig> httpFilterConfigs) {
        this.httpFilterConfigs = httpFilterConfigs != null ? new ArrayList<>(httpFilterConfigs) : null;
    }

    @Override
    public String toString() {
        return "HttpConnectionManager{" + "httpMaxStreamDurationNano=" + httpMaxStreamDurationNano + ", " + "rdsName="
                + rdsName + ", " + "virtualHosts=" + virtualHosts + ", " + "httpFilterConfigs=" + httpFilterConfigs
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HttpConnectionManager that = (HttpConnectionManager) o;
        return httpMaxStreamDurationNano == that.httpMaxStreamDurationNano
                && Objects.equals(rdsName, that.rdsName)
                && Objects.equals(virtualHosts, that.virtualHosts)
                && Objects.equals(httpFilterConfigs, that.httpFilterConfigs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpMaxStreamDurationNano, rdsName, virtualHosts, httpFilterConfigs);
    }

    public static HttpConnectionManager forRdsName(
            long httpMaxStreamDurationNano, String rdsName, @Nullable List<NamedFilterConfig> httpFilterConfigs) {
        Assert.notNull(rdsName, "rdsName must not be null");
        return create(httpMaxStreamDurationNano, rdsName, null, httpFilterConfigs);
    }

    public static HttpConnectionManager forVirtualHosts(
            long httpMaxStreamDurationNano,
            List<VirtualHost> virtualHosts,
            @Nullable List<NamedFilterConfig> httpFilterConfigs) {
        Assert.notNull(virtualHosts, "virtualHosts must not be null");
        return create(httpMaxStreamDurationNano, null, virtualHosts, httpFilterConfigs);
    }

    private static HttpConnectionManager create(
            long httpMaxStreamDurationNano,
            @Nullable String rdsName,
            @Nullable List<VirtualHost> virtualHosts,
            @Nullable List<NamedFilterConfig> httpFilterConfigs) {
        return new HttpConnectionManager(httpMaxStreamDurationNano, rdsName, virtualHosts, httpFilterConfigs);
    }
}
