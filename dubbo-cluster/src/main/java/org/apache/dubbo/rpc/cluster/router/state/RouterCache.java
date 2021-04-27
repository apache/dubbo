package org.apache.dubbo.rpc.cluster.router.state;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.rpc.Invoker;

public class RouterCache {
    protected ConcurrentHashMap<String, BitList<Invoker>> addrPool = new ConcurrentHashMap<>();
    protected Object addrMetadata;

    public ConcurrentHashMap<String, BitList<Invoker>> getAddrPool() {
        return addrPool;
    }

    public void setAddrPool(
        ConcurrentHashMap<String, BitList<Invoker>> addrPool) {
        this.addrPool = addrPool;
    }

    public Object getAddrMetadata() {
        return addrMetadata;
    }

    public void setAddrMetadata(Object addrMetadata) {
        this.addrMetadata = addrMetadata;
    }
}