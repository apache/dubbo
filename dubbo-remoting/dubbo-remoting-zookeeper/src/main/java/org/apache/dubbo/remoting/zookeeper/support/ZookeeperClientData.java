package org.apache.dubbo.remoting.zookeeper.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;

import java.util.Set;

/**
 * 2019/1/10
 */
public class ZookeeperClientData {
    public ZookeeperClientData(ZookeeperClient zookeeperClient, Set<URL> originalURLs) {
        this.zookeeperClient = zookeeperClient;
        this.originalURLs = originalURLs;
    }

    public void fillOriginalURL(URL url) {
        originalURLs.add(url);
    }

    ZookeeperClient zookeeperClient;
    Set<URL> originalURLs;

    public ZookeeperClient getZookeeperClient() {
        return zookeeperClient;
    }

    @Override
    public String toString() {
        return "ZookeeperClientData{" +
                "zookeeperClient=" + zookeeperClient +
                ", originalURLs=" + originalURLs +
                '}';
    }


}
