package com.alibaba.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;

/**
 * 2019-04-16
 */
@Deprecated
public abstract class AbstractRegistryFactory extends org.apache.dubbo.registry.support.AbstractRegistryFactory {


    protected abstract com.alibaba.dubbo.registry.Registry createRegistry(com.alibaba.dubbo.common.URL url);

    protected Registry createRegistry(URL url) {
        return createRegistry(new com.alibaba.dubbo.common.URL(url));
    }
}
