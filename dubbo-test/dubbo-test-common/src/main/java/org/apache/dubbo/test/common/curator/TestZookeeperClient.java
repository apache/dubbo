package org.apache.dubbo.test.common.curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * zookeeper 客户端测试
 */
public class TestZookeeperClient {
    public static void main(String[] args) throws Exception {

        // Zookeeper集群地址，多个节点地址可以用逗号分隔

        String zkAddress = "127.0.0.1:2181";

        // 重试策略，如果连接不上ZooKeeper集群，会重试三次，重试间隔会递增

        RetryPolicy retryPolicy =

            new ExponentialBackoffRetry(1000, 3);

        // 创建Curator Client并启动，启动成功之后，就可以与Zookeeper进行交互了

        CuratorFramework client =

            CuratorFrameworkFactory.newClient(zkAddress, retryPolicy);

        client.start();

        // 下面简单说明Curator中常用的API

        // create()方法创建ZNode，可以调用额外方法来设置节点类型、添加Watcher

        // 下面是创建一个名为"user"的持久节点，其中会存储一个test字符串

        String path = client.create().withMode(CreateMode.PERSISTENT)

            .forPath("/user", "test".getBytes());

        System.out.println(path);

        // 输出:/user

        // checkExists()方法可以检查一个节点是否存在

        Stat stat = client.checkExists().forPath("/user");

        System.out.println(stat!=null);

        // 输出:true，返回的Stat不为null，即表示节点存在

        // getData()方法可以获取一个节点中的数据

        byte[] data = client.getData().forPath("/user");

        System.out.println(new String(data));

        // 输出:test

        // setData()方法可以设置一个节点中的数据

        stat = client.setData().forPath("/user","data".getBytes());

        data = client.getData().forPath("/user");

        System.out.println(new String(data));

        // 输出:data

        // 在/user节点下，创建多个临时顺序节点

        for (int i = 0; i < 3; i++) {

            client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)

                .forPath("/user/child-");

        }

        // 获取所有子节点

        List<String> children = client.getChildren().forPath("/user");

        System.out.println(children);

        // 输出：[child-0000000002, child-0000000001, child-0000000000]

        // delete()方法可以删除指定节点，deletingChildrenIfNeeded()方法

        // 会级联删除子节点

        client.delete().deletingChildrenIfNeeded().forPath("/user");


    }
}
