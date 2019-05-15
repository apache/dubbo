package org.apache.dubbo.registry.multiple;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.redis.RedisRegistry;
import org.apache.dubbo.registry.zookeeper.ZookeeperRegistry;
import org.apache.dubbo.remoting.zookeeper.ZookeeperClient;
import org.apache.dubbo.remoting.zookeeper.curator.CuratorZookeeperClient;

import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.embedded.RedisServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 2019-04-30
 */
public class DynamicMultipleRegistryTest {

    private static final String SERVICE_NAME = "org.apache.dubbo.registry.AbstractDynamicMultipleService";
    private static final String SERVICE2_NAME = "org.apache.dubbo.registry.AbstractDynamicMultipleService2";
    private static final String SERVICE3_NAME = "org.apache.dubbo.registry.AbstractDynamicMultipleService3";

    private static TestingServer zkServer;
    private static TestingServer zkServer2;
    private static RedisServer redisServer;
    static int zkServerPort;
    static int zkServerPort2;
    static int redisServerPort;

    private static String zookeeperRegistryURLStr;
    private static String zookeeperRegistryURLStr2;
    private static String redisRegistryURLStr;

    private static AbstractDynamicMultipleRegistry multipleRegistry;
    private static AbstractDynamicMultipleRegistry multipleRegistry2;
    private static AbstractDynamicMultipleRegistry multipleRegistry3;
    // for test content
    private static ZookeeperClient zookeeperClient;

    private static ZookeeperRegistry zookeeperRegistry;
    private static RedisRegistry redisRegistry;


    @BeforeAll
    public static void setUp() throws Exception {
        zkServerPort = NetUtils.getAvailablePort();
        zkServerPort2 = NetUtils.getAvailablePort();
        zkServer = new TestingServer(zkServerPort, true);
        zookeeperRegistryURLStr = "zookeeper://127.0.0.1:" + zkServerPort;

        zkServer2 = new TestingServer(zkServerPort2, true);
        zookeeperRegistryURLStr2 = "zookeeper://127.0.0.1:" + zkServerPort2;

        redisServerPort = NetUtils.getAvailablePort();
        redisServer = new RedisServer(redisServerPort);
        redisServer.start();
        redisRegistryURLStr = "redis://127.0.0.1:" + redisServerPort;


        URL url = URL.valueOf("multiple://127.0.0.1?application=vic&" +
                MultipleRegistry.REGISTRY_FOR_SERVICE + "=" + zookeeperRegistryURLStr + "," + redisRegistryURLStr + "&"
                + MultipleRegistry.REGISTRY_FOR_REFERENCE + "=" + zookeeperRegistryURLStr + "," + redisRegistryURLStr);
        URL url3 = URL.valueOf("multiple://127.0.0.1?application=vic&" +
                MultipleRegistry.REGISTRY_FOR_SERVICE + "=" + zookeeperRegistryURLStr2 + "&"
                + MultipleRegistry.REGISTRY_FOR_REFERENCE + "=" + zookeeperRegistryURLStr2);
        multipleRegistry = new AbstractDynamicMultipleRegistry(url);
        multipleRegistry2 = new AbstractDynamicMultipleRegistry(url);
        multipleRegistry3 = new AbstractDynamicMultipleRegistry(url3);

        // for test validation
        zookeeperClient = new CuratorZookeeperClient(URL.valueOf(zookeeperRegistryURLStr));
        zookeeperRegistry = MultipleRegistryTestUtil.getZookeeperRegistry(multipleRegistry.getServiceRegistries().values());
        redisRegistry = MultipleRegistryTestUtil.getRedisRegistry(multipleRegistry.getServiceRegistries().values());
    }

    @AfterAll
    public static void tearDown() throws Exception {
        zkServer.stop();
        redisServer.stop();
    }

    @Test
    public void testRefreshServiceOnly() throws InterruptedException {
        URL serviceUrl = URL.valueOf("http2://multiple/" + SERVICE_NAME + "?notify=false&methods=test1,test2&category=providers");
//        URL serviceUrl2 = URL.valueOf("http2://multiple2/" + SERVICE_NAME + "?notify=false&methods=test1,test2&category=providers");
        multipleRegistry.register(serviceUrl);

        String path = "/dubbo/" + SERVICE_NAME + "/providers";
        doAssertServiceData(path, serviceUrl.toFullString(), serviceUrl.toFullString(), serviceUrl.toFullString());


        multipleRegistry.refreshServiceRegistry(Arrays.asList(zookeeperRegistryURLStr));
        doAssertServiceData(path, serviceUrl.toFullString(), serviceUrl.toFullString(), null);

        multipleRegistry.refreshServiceRegistry(Arrays.asList(redisRegistryURLStr));
        doAssertServiceData(path, serviceUrl.toFullString(), null, serviceUrl.toFullString());

        multipleRegistry.refreshServiceRegistry(Arrays.asList(redisRegistryURLStr, zookeeperRegistryURLStr));
        doAssertServiceData(path, serviceUrl.toFullString(), serviceUrl.toFullString(), serviceUrl.toFullString());
    }

    @Test
    public void testRefreshReferenceOnly() throws InterruptedException {
        URL serviceUrl = URL.valueOf("http2://multiple/" + SERVICE2_NAME + "?notify=false&methods=test1,test2&category=providers");
//        URL serviceUrl2 = URL.valueOf("http2://multiple2/" + SERVICE_NAME + "?notify=false&methods=test1,test2&category=providers");
        multipleRegistry.register(serviceUrl);

        String path = "/dubbo/" + SERVICE2_NAME + "/providers";
        doAssertServiceData(path, serviceUrl.toFullString(), serviceUrl.toFullString(), serviceUrl.toFullString());

        final List<URL> list = new ArrayList<URL>();
        multipleRegistry.subscribe(serviceUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                System.out.println("invoke notify: " + urls);
                list.clear();
                list.addAll(urls);
            }
        });

        Thread.sleep(1200);
        Assertions.assertTrue(list.size() == 2);

        multipleRegistry.refreshReferenceRegistry(Arrays.asList(redisRegistryURLStr));
        Thread.sleep(1200);
        doAssertReferenceData(list, 1, "http2");

        multipleRegistry.refreshReferenceRegistry(Arrays.asList(zookeeperRegistryURLStr));
        Thread.sleep(1200);
        doAssertReferenceData(list, 1, "http2");

        multipleRegistry.refreshReferenceRegistry(Arrays.asList(zookeeperRegistryURLStr, redisRegistryURLStr));
        Thread.sleep(1200);
        doAssertReferenceData(list, 2, "http2");

        // No Subscription, it will not effective.
        multipleRegistry.refreshReferenceRegistry(Collections.EMPTY_LIST);
        Thread.sleep(1200);
        doAssertReferenceData(list, 2, "http2");
    }

    @Test
    public void testRefreshServiceAndReference() throws InterruptedException {
        URL serviceUrl = URL.valueOf("http2://multiple/" + SERVICE3_NAME + "?notify=false&methods=test1,test2&category=providers");
//        URL serviceUrl2 = URL.valueOf("http2://multiple2/" + SERVICE_NAME + "?notify=false&methods=test1,test2&category=providers");
        multipleRegistry2.register(serviceUrl);
        multipleRegistry3.register(serviceUrl);

        String path = "/dubbo/" + SERVICE3_NAME + "/providers";
        doAssertServiceData(path, serviceUrl.toFullString(), serviceUrl.toFullString(), serviceUrl.toFullString());

        final List<URL> list = new ArrayList<URL>();
        multipleRegistry2.subscribe(serviceUrl, new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                System.out.println("invoke notify: " + urls);
                list.clear();
                list.addAll(urls);
            }
        });

        Thread.sleep(600);
        Assertions.assertTrue(list.size() == 2);
        doAssertReferenceData(list, 2, "http2");

        // // refer: zk, redis ;  service: zk
        multipleRegistry2.refreshServiceRegistry(Arrays.asList(zookeeperRegistryURLStr));
        Thread.sleep(500);
        doAssertServiceData(path, serviceUrl.toFullString(), serviceUrl.toFullString(), null);
        doAssertReferenceData(list, 1, "http2");

        // refer: redis ;  service: zk
        multipleRegistry2.refreshReferenceRegistry(Arrays.asList(redisRegistryURLStr));
        Thread.sleep(500);
        doAssertReferenceData(list, 1, null);

        // refer: redis ;  service: redis
        multipleRegistry2.refreshServiceRegistry(Arrays.asList(redisRegistryURLStr));
        Thread.sleep(500);
        doAssertServiceData(path, serviceUrl.toFullString(), null, serviceUrl.toFullString());
        doAssertReferenceData(list, 1, "http2");

        // refer: redis ;  service: zk, zk2
        multipleRegistry2.refreshServiceRegistry(Arrays.asList(zookeeperRegistryURLStr2, zookeeperRegistryURLStr));
        Thread.sleep(500);
        doAssertServiceData(path, serviceUrl.toFullString(), serviceUrl.toFullString(), null);
        doAssertReferenceData(list, 1, null);

        // refer: zk, zk2 ;  service: zk, zk2
        multipleRegistry2.refreshReferenceRegistry(Arrays.asList(zookeeperRegistryURLStr2, zookeeperRegistryURLStr));
        Thread.sleep(500);
        doAssertReferenceData(list, 2, "http2");
    }

    private void doAssertReferenceData(List<URL> list, int size, String protocol) {
        if (size == 0) {
            Assertions.assertTrue(list.size() == size);
            return;
        }
        Assertions.assertTrue(list.size() == size);
        List<URL> urls = MultipleRegistryTestUtil.getProviderURLsFromNotifyURLS(list);
        Assertions.assertTrue(list.size() == size);
        if (protocol == null) {
            Assertions.assertTrue("empty".equals(list.get(0).getProtocol()));
        } else {
            Assertions.assertTrue(protocol.equals(list.get(0).getProtocol()));
        }
    }

    private void doAssertServiceData(String path, String content, String zookeeperContent, String redisContent) {
        List<String> providerList = zookeeperClient.getChildren(path);

        if (zookeeperContent != null) {
            Assertions.assertTrue(!providerList.isEmpty());
            Assertions.assertEquals(providerList.get(0), URL.encode(zookeeperContent));
        } else {
            Assertions.assertTrue(providerList.isEmpty());
        }
        if (redisContent != null) {
            Assertions.assertNotNull(MultipleRegistryTestUtil.getRedisHashContent(redisServerPort, path, content));
        } else {
            Assertions.assertNull(MultipleRegistryTestUtil.getRedisHashContent(redisServerPort, path, content));
        }
    }

}
