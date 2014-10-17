package com.alibaba.dubbo.rpc.protocol;

/**
 * TODO this is just a workround for rest protocol, and now we just ensure it works in the most common dubbo usages
 *
 * @author lishen
 */
public class ServiceImplHolder {

    private static final ServiceImplHolder INSTANCE = new ServiceImplHolder();

    private final ThreadLocal holder  = new ThreadLocal();

    public static ServiceImplHolder getInstance() {
        return INSTANCE;
    }

    private ServiceImplHolder() {
    }

    public Object popServiceImpl() {
        Object impl = holder.get();
        holder.remove();
        return impl;
    }

    public void pushServiceImpl(Object impl) {
        holder.set(impl);
    }
}
