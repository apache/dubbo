package org.apache.dubbo.rpc.cluster.xds.resource;

import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.List;

public class XdsCluster<T> {
    private String name;

    private String lbPolicy;

    private List<XdsEndpoint> xdsEndpoints;

    public BitList<Invoker<T>> getInvokers() {
        return invokers;
    }

    public void setInvokers(BitList<Invoker<T>> invokers) {
        this.invokers = invokers;
    }

    private BitList<Invoker<T>> invokers;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLbPolicy() {
        return lbPolicy;
    }

    public void setLbPolicy(String lbPolicy) {
        this.lbPolicy = lbPolicy;
    }

    public List<XdsEndpoint> getXdsEndpoints() {
        return xdsEndpoints;
    }

    public void setXdsEndpoints(List<XdsEndpoint> xdsEndpoints) {
        this.xdsEndpoints = xdsEndpoints;
    }
}
