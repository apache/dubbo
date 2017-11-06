package com.alibaba.dubbo.remoting.zookeeper.curator;

import com.alibaba.dubbo.common.URL;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author ken.lj
 * @date 2017/10/16
 */
@Ignore
public class CuratorZookeeperClientTest {

    @Test
    public void testCheckExists() {
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService"));
        String path = "/dubbo/com.alibaba.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);
        Assert.assertTrue(curatorClient.checkExists(path));
        Assert.assertFalse(curatorClient.checkExists(path + "/noneexits"));
    }

    /**
     * create checkExists 性能測試
     */
    @Test
    public void testCreate() {
        CuratorZookeeperClient curatorClient = new CuratorZookeeperClient(URL.valueOf("zookeeper://127.0.0.1:2181/com.alibaba.dubbo.registry.RegistryService"));
        String path = "/dubbo/com.alibaba.dubbo.demo.DemoService/providers";
        curatorClient.create(path, false);

        // 重复create 100次，耗时
        long startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            curatorClient.create(path, true);
        }
        System.out.println("create cost: " + (System.nanoTime() - startTime) / 1000 / 1000);

        // 判断100次，耗时
        startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            curatorClient.checkExists(path);
        }
        System.out.println("judge cost: " + (System.nanoTime() - startTime) / 1000 / 1000);
    }
}
