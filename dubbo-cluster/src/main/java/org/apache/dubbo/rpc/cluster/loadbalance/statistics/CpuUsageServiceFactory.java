package org.apache.dubbo.rpc.cluster.loadbalance.statistics;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;

public class CpuUsageServiceFactory {

    private Protocol protocol;

    private ProxyFactory proxyFactory;

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public CpuUsageService createCpuUsageService(URL url) {
        return null;
    }
}
