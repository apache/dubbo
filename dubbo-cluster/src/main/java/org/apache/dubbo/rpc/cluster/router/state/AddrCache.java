package org.apache.dubbo.rpc.cluster.router.state;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.dubbo.rpc.Invoker;

public class AddrCache {
    protected List<Invoker> invokers;
    protected ConcurrentHashMap<String, RouterCache> cache = new ConcurrentHashMap<>();

    public List<Invoker> getInvokers() {
        return invokers;
    }

    public void setInvokers(List<Invoker> invokers) {
        this.invokers = invokers;
    }

    public ConcurrentHashMap<String, RouterCache> getCache() {
        return cache;
    }

    public void setCache(
        ConcurrentHashMap<String, RouterCache> cache) {
        this.cache = cache;
    }
}
