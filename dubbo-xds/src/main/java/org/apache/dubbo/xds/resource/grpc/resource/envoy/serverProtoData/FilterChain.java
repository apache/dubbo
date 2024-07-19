package org.apache.dubbo.xds.resource.grpc.resource.envoy.serverProtoData;

//import org.apache.dubbo.xds.resource.grpc.SslContextProviderSupplier;

import org.apache.dubbo.xds.resource.grpc.EnvoyServerProtoData;
import org.apache.dubbo.xds.resource.grpc.EnvoyServerProtoData.DownstreamTlsContext;
import org.apache.dubbo.xds.resource.grpc.SslContextProviderSupplier;
import org.apache.dubbo.xds.resource.grpc.TlsContextManager;

import java.util.Objects;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

public class FilterChain {

    private String name;
    private FilterChainMatch filterChainMatch;
    private HttpConnectionManager httpConnectionManager;
    //  private SslContextProviderSupplier sslContextProviderSupplier;

    public FilterChain(
            String name, FilterChainMatch filterChainMatch, HttpConnectionManager httpConnectionManager
            /*SslContextProviderSupplier sslContextProviderSupplier*/) {
        if (name == null) {
            throw new NullPointerException("Null name");
        }
        this.name = name;
        if (filterChainMatch == null) {
            throw new NullPointerException("Null filterChainMatch");
        }
        this.filterChainMatch = filterChainMatch;
        if (httpConnectionManager == null) {
            throw new NullPointerException("Null httpConnectionManager");
        }
        this.httpConnectionManager = httpConnectionManager;
        //    this.sslContextProviderSupplier = sslContextProviderSupplier;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FilterChainMatch filterChainMatch() {
        return filterChainMatch;
    }

    public void setFilterChainMatch(FilterChainMatch filterChainMatch) {
        this.filterChainMatch = filterChainMatch;
    }

    public HttpConnectionManager httpConnectionManager() {
        return httpConnectionManager;
    }

    public void setHttpConnectionManager(HttpConnectionManager httpConnectionManager) {
        this.httpConnectionManager = httpConnectionManager;
    }

/*
  public SslContextProviderSupplier getSslContextProviderSupplier() {
    return sslContextProviderSupplier;
  }

  public void setSslContextProviderSupplier(SslContextProviderSupplier sslContextProviderSupplier) {
    this.sslContextProviderSupplier = sslContextProviderSupplier;
  }
*/

    public String toString() {
        return "FilterChain{" + "name=" + name + ", " + "filterChainMatch=" + filterChainMatch + ", "
                + "httpConnectionManager=" + httpConnectionManager + ", "
                //        + "sslContextProviderSupplier=" + sslContextProviderSupplier
                + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        FilterChain that = (FilterChain) o;
        return Objects.equals(name, that.name) && Objects.equals(filterChainMatch, that.filterChainMatch)
                && Objects.equals(httpConnectionManager, that.httpConnectionManager);
        //        && Objects.equals(sslContextProviderSupplier, that.sslContextProviderSupplier);
    }

    public int hashCode() {
        return Objects.hash(name, filterChainMatch, httpConnectionManager/*, sslContextProviderSupplier*/);
    }

    public static FilterChain create(
            String name,
            FilterChainMatch filterChainMatch,
            HttpConnectionManager httpConnectionManager/*,
            @Nullable DownstreamTlsContext downstreamTlsContext,
            TlsContextManager tlsContextManager*/) {
//        SslContextProviderSupplier sslContextProviderSupplier =
//                downstreamTlsContext == null
//                        ? null : new SslContextProviderSupplier(downstreamTlsContext, tlsContextManager);
        return new FilterChain(
                name, filterChainMatch, httpConnectionManager/*, sslContextProviderSupplier*/);
    }
}
