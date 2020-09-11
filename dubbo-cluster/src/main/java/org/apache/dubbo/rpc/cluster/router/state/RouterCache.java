package org.apache.dubbo.rpc.cluster.router.state;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.common.utils.BitList;
import org.apache.dubbo.rpc.Invoker;

public class RouterCache {
    protected ConcurrentHashMap<String, BitList<Invoker>> addrPool;
    protected String addrMetadata;

    public ConcurrentHashMap<String, BitList<Invoker>> getAddrPool() {
        return addrPool;
    }

    public void setAddrPool(
        ConcurrentHashMap<String, BitList<Invoker>> addrPool) {
        this.addrPool = addrPool;
    }

    public String getAddrMetadata() {
        return addrMetadata;
    }

    public void setAddrMetadata(String addrMetadata) {
        this.addrMetadata = addrMetadata;
    }
}