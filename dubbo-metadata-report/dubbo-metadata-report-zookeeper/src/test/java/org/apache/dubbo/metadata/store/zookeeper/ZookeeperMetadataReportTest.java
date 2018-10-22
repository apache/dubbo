package org.apache.dubbo.metadata.store.zookeeper;

import org.apache.curator.test.TestingServer;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.remoting.zookeeper.curator.CuratorZookeeperTransporter;
import org.apache.dubbo.rpc.RpcException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 *  2018/10/9
 */
public class ZookeeperMetadataReportTest {
    private TestingServer zkServer;
    private ZookeeperMetadataReport zookeeperServiceStore;
    private URL registryUrl;
    private ZookeeperMetadataReportFactory zookeeperServiceStoreFactory;

    @Before
    public void setUp() throws Exception {
        int zkServerPort = NetUtils.getAvailablePort();
        this.zkServer = new TestingServer(zkServerPort, true);
        this.registryUrl = URL.valueOf("zookeeper://localhost:" + zkServerPort);

        zookeeperServiceStoreFactory = new ZookeeperMetadataReportFactory();
        zookeeperServiceStoreFactory.setZookeeperTransporter(new CuratorZookeeperTransporter());
        this.zookeeperServiceStore = (ZookeeperMetadataReport) zookeeperServiceStoreFactory.createServiceStore(registryUrl);
    }

    @After
    public void tearDown() throws Exception {
        zkServer.stop();
    }

    @Test
    public void testToCategoryPathWithNormal() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.ZkService?version=1.55.88&application=zktest&side=provider");
        String categoryUrl = zookeeperServiceStore.toCategoryPath(url);
        Assert.assertEquals(categoryUrl, "/dubbo/org.apache.dubbo.ZkService/" + zookeeperServiceStore.TAG + "/1.55.88/provider");
    }

    @Test
    public void testToCategoryPathNoVersion() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.ZkService?application=zktestNoV&side=provider");
        String categoryUrl = zookeeperServiceStore.toCategoryPath(url);
        Assert.assertEquals(categoryUrl, "/dubbo/org.apache.dubbo.ZkService/" + zookeeperServiceStore.TAG + "/provider");
    }

    @Test
    public void testToCategoryPathNoSide() {
        URL url = URL.valueOf("consumer://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.ZkService?version=1.55.88&application=zktestNoSide");
        String categoryUrl = zookeeperServiceStore.toCategoryPath(url);
        Assert.assertEquals(categoryUrl, "/dubbo/org.apache.dubbo.ZkService/" + zookeeperServiceStore.TAG + "/1.55.88/consumer/zktestNoSide");
    }

    @Test
    public void testToCategoryPathNoSideNoVersion() {
        URL url = URL.valueOf("consumer://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.ZkService?application=zktestNoSideVersion");
        String categoryUrl = zookeeperServiceStore.toCategoryPath(url);
        Assert.assertEquals(categoryUrl, "/dubbo/org.apache.dubbo.ZkService/" + zookeeperServiceStore.TAG + "/consumer/zktestNoSideVersion");
    }

    @Test
    public void testDoPut() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.ZkService?version=1.55.8899&application=zktestPut&side=provider");
        zookeeperServiceStore.doPut(url);

        try {
            String path = zookeeperServiceStore.toCategoryPath(url);
            List<String> resultStr = zookeeperServiceStore.zkClient.getChildren(path);
            Assert.assertTrue(resultStr.size() == 1);
            URL result = new URL("dubbo","127.0.0.1", 8090);
            Assert.assertTrue(result.getParameters().isEmpty());
            result = result.addParameterString(URL.decode(resultStr.get(0)));
            Assert.assertFalse(result.getParameters().isEmpty());
            Assert.assertEquals(result.getParameter("version"),"1.55.8899");
            Assert.assertEquals(result.getParameter("application"),"zktestPut");
            Assert.assertEquals(result.getParameter("side"),"provider");
        } catch (Throwable e) {
            throw new RpcException("Failed to put " + url + " to redis . cause: " + e.getMessage(), e);
        }
    }

    @Test
    public void testDoPeekProvider() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.ZkService?version=1.55.8899&application=zktestPut&side=provider");
        zookeeperServiceStore.doPut(url);

        try {
            URL result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.ZkService?version=1.55.8899&application=zkDoPeek&side=provider");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertFalse(result.getParameters().isEmpty());
            Assert.assertEquals(result.getParameter("version"),"1.55.8899");
            Assert.assertEquals(result.getParameter("application"),"zktestPut");
            Assert.assertEquals(result.getParameter("side"),"provider");


            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.TestDD?version=1.55.8899&application=zkDoPeek&side=provider");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertNull(result);

            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.ZkService?version=1.55.8000&application=zkDoPeek&side=provider");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertNull(result);

            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.ZkService?version=1.55.8899&application=zkDoPeek&side=consumer");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertNull(result);

        } catch (Throwable e) {
            throw new RpcException("Failed to put " + url + " to redis . cause: " + e.getMessage(), e);
        }
    }

    @Test
    public void testDoPeekConsumer() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.ZkServicePeek2?version=1.55.8899&application=zkDoPeek2&side=consumer");
        zookeeperServiceStore.doPut(url);

        try {
            URL result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.ZkServicePeek2?version=1.55.8899&application=zkDoPeek2&side=consumer");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertFalse(result.getParameters().isEmpty());
            Assert.assertEquals(result.getParameter("version"),"1.55.8899");
            Assert.assertEquals(result.getParameter("application"),"zkDoPeek2");
            Assert.assertEquals(result.getParameter("side"),"consumer");


            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.ZkServicePeek2?version=1.55.8899&application=zkDoPeek&side=consumer");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertNull(result);

            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.TestDDw?version=1.55.8899&application=zkDoPeek&side=consumer");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertNull(result);

            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.ZkServicePeek2?version=1.55.8000&application=zkDoPeek&side=consumer");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertNull(result);

            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.ZkServicePeek2?version=1.55.8899&application=zkDoPeek&side=provider");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertNull(result);

        } catch (Throwable e) {
            throw new RpcException("Failed to put " + url + " to redis . cause: " + e.getMessage(), e);
        }
    }

    @Test
    public void testDoPeekConsumerPutTwice() {
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.ZkServicePeek2?version=1.55.8899&application=zkDoPeekT1&side=consumer");
        zookeeperServiceStore.doPut(url);
        URL url2 = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.ZkServicePeek2?version=1.55.8899&application=zkDoPeekT2&side=consumer");
        zookeeperServiceStore.doPut(url2);

        try {
            URL result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.ZkServicePeek2?version=1.55.8899&application=zkDoPeekT1&side=consumer");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertFalse(result.getParameters().isEmpty());
            Assert.assertEquals(result.getParameter("version"),"1.55.8899");
            Assert.assertEquals(result.getParameter("application"),"zkDoPeekT1");
            Assert.assertEquals(result.getParameter("side"),"consumer");


            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.ZkServicePeek2?version=1.55.8899&application=zkDoPeekT2&side=consumer");
            result = zookeeperServiceStore.doPeek(result);
            Assert.assertFalse(result.getParameters().isEmpty());
            Assert.assertEquals(result.getParameter("version"),"1.55.8899");
            Assert.assertEquals(result.getParameter("application"),"zkDoPeekT2");
            Assert.assertEquals(result.getParameter("side"),"consumer");

        } catch (Throwable e) {
            throw new RpcException("Failed to put " + url + " to redis . cause: " + e.getMessage(), e);
        }
    }
}
