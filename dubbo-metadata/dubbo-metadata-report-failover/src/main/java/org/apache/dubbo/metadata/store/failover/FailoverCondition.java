package org.apache.dubbo.metadata.store.failover;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;

@SPI("failover")
public interface FailoverCondition {

    /**
     * Whether metadata should be reported.
     *
     * @param url registry url, eg: zookeeper://127.0.0.1:2181
     * @return true store metadata to the specified URL.
     */
    boolean shouldRegister(URL url);

    /**
     * Whether metadata should be read from specified url.
     *
     * @param url registry url, eg: zookeeper://127.0.0.1:2181
     * @return true read metadata from specified URL.
     */
    boolean shouldQuery(URL url);

    /**
     * Judge whether it is a local region or a local datacenter.
     * <p>
     * Allows the local region or datacenter to be read first.
     *
     * @param url
     * @return
     */
    boolean isLocalDataCenter(URL url);

}