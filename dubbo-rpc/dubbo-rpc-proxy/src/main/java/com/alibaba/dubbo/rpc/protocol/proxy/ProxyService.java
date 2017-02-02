package com.alibaba.dubbo.rpc.protocol.proxy;

import java.lang.reflect.Type;

/**
 * Created by wuyu on 2016/7/14.
 */
public interface ProxyService {

    /**
     * 用于 restProxy client 代理使用，服务端无法使用
     * @param service
     * @param <T>
     * @return
     */
    public <T> T target(Class<T> service);

    /**
     *  用于 restProxy server 端使用，只支持dubbo协议。方便泛化代理使用
     * @param service
     * @param group
     * @param version
     * @param <T>
     * @return
     */
    public <T> T target(Class<T> service, String group, String version);

    /**
     * 泛化调用
     * @param config
     * @param returnClass
     * @return
     */
    public Object invoke(GenericServiceConfig config, Type returnClass);

}
