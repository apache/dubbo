package org.apache.dubbo.metadata.store.zookeeper;

/**
 * 2018/10/26
 */
public interface ZookeeperMetadataReport4TstService {

    int getCounter();

    void printResult(String var);
}
