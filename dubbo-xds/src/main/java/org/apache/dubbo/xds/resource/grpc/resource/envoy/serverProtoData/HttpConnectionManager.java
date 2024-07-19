package org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData;

import org.apache.dubbo.xds.resource.grpc.resource.VirtualHost;
import org.apache.dubbo.xds.resource.grpc.resource.filter.NamedFilterConfig;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

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
        this.virtualHosts = virtualHosts != null ? new ArrayList<>(virtualHosts) : null;
        this.httpFilterConfigs = httpFilterConfigs != null ? new ArrayList<>(httpFilterConfigs) : null;
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
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        HttpConnectionManager that = (HttpConnectionManager) o;
        return httpMaxStreamDurationNano == that.httpMaxStreamDurationNano && Objects.equals(rdsName, that.rdsName)
                && Objects.equals(virtualHosts, that.virtualHosts)
                && Objects.equals(httpFilterConfigs, that.httpFilterConfigs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpMaxStreamDurationNano, rdsName, virtualHosts, httpFilterConfigs);
    }

    public static HttpConnectionManager forRdsName(
            long httpMaxStreamDurationNano, String rdsName, @Nullable List<NamedFilterConfig> httpFilterConfigs) {
        checkNotNull(rdsName, "rdsName");
        return create(httpMaxStreamDurationNano, rdsName, null, httpFilterConfigs);
    }

    public static HttpConnectionManager forVirtualHosts(
            long httpMaxStreamDurationNano,
            List<VirtualHost> virtualHosts,
            @Nullable List<NamedFilterConfig> httpFilterConfigs) {
        checkNotNull(virtualHosts, "virtualHosts");
        return create(httpMaxStreamDurationNano, null, virtualHosts, httpFilterConfigs);
    }

    private static HttpConnectionManager create(
            long httpMaxStreamDurationNano,
            @Nullable String rdsName,
            @Nullable List<VirtualHost> virtualHosts,
            @Nullable List<NamedFilterConfig> httpFilterConfigs) {
        return new HttpConnectionManager(httpMaxStreamDurationNano, rdsName,
                virtualHosts == null ? null : ImmutableList.copyOf(virtualHosts),
                httpFilterConfigs == null ? null : ImmutableList.copyOf(httpFilterConfigs));
    }
}
