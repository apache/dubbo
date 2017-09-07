package com.alibaba.dubbo.remoting.zookeeper.zkclient;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperClient;
import com.alibaba.dubbo.remoting.zookeeper.ZookeeperTransporter;

public class ZkclientZookeeperTransporter implements ZookeeperTransporter {

    public ZookeeperClient connect(URL url) {
        return new ZkclientZookeeperClient(url);
    }

}
