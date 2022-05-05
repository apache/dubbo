package com.alibaba.dubbo.rpc.cluster;

import org.apache.dubbo.common.URL;

public interface Configurator extends org.apache.dubbo.rpc.cluster.Configurator {
    /**
     * Get the configurator url.
     *
     * @return configurator url.
     */
    com.alibaba.dubbo.common.URL getUrl();

    /**
     * Configure the provider url.
     *
     * @param url - old provider url.
     * @return new provider url.
     */
    com.alibaba.dubbo.common.URL configure(com.alibaba.dubbo.common.URL url);

    @Override
    default URL configure(URL url) {
        return this.configure(new com.alibaba.dubbo.common.URL(url));
    }
}
