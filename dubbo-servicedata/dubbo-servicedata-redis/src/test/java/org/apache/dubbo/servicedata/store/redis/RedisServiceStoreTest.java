package org.apache.dubbo.servicedata.store.redis;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.rpc.RpcException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * @author cvictory ON 2018/10/9
 */
public class RedisServiceStoreTest {
    RedisServiceStore redisServiceStore;
    RedisServer redisServer;

    @Before
    public void constructor() throws IOException {
        int redisPort = NetUtils.getAvailablePort();
        this.redisServer = new RedisServer(redisPort);
        this.redisServer.start();
        URL registryUrl = URL.valueOf("redis://localhost:" + redisPort);
        redisServiceStore = (RedisServiceStore) new RedisServiceStoreFactory().createServiceStore(registryUrl);
    }

    @After
    public void tearDown() throws Exception {
        this.redisServer.stop();
    }

    @Test
    public void testGetProtocol(){
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic&side=provider");
        String protocol = redisServiceStore.getProtocol(url);
        Assert.assertEquals(protocol, "provider");

        URL url2 = URL.valueOf("consumer://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.0&application=vic");
        String protocol2 = redisServiceStore.getProtocol(url2);
        Assert.assertEquals(protocol2, "consumer");
    }

    @Test
    public void testDoPut(){
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.9&application=vicss&side=provider");
        redisServiceStore.doPutService(url);

        try {
            Jedis jedis = redisServiceStore.pool.getResource();
            String value = jedis.get(redisServiceStore.getKey(url));
            URL result = new URL("dubbo","127.0.0.1", 8090);
            Assert.assertTrue(result.getParameters().isEmpty());
            result = result.addParameterString(value);
            Assert.assertFalse(result.getParameters().isEmpty());
            Assert.assertEquals(result.getParameter("version"),"1.0.9");
            Assert.assertEquals(result.getParameter("application"),"vicss");
            Assert.assertEquals(result.getParameter("side"),"provider");
        } catch (Throwable e) {
            throw new RpcException("Failed to put " + url + " to redis . cause: " + e.getMessage(), e);
        } finally {
            redisServiceStore.pool.close();
        }
    }

    @Test
    public void testDoPeek(){
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.9&application=vicss&side=provider");
        redisServiceStore.doPutService(url);

        try {
            URL result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.TestService?version=1.0.9&application=vicww&side=provider");
            result = redisServiceStore.doPeekService(result);
            Assert.assertFalse(result.getParameters().isEmpty());
            Assert.assertEquals(result.getParameter("version"),"1.0.9");
            Assert.assertEquals(result.getParameter("application"),"vicss");
            Assert.assertEquals(result.getParameter("side"),"provider");


            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.TestDD?version=1.0.9&application=vicss&side=provider");
            result = redisServiceStore.doPeekService(result);
            Assert.assertNull(result);

            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.TestService?version=1.0.999&application=vicss&side=provider");
            result = redisServiceStore.doPeekService(result);
            Assert.assertNull(result);

            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.TestService?version=1.0.9&application=vicss&side=consumer");
            result = redisServiceStore.doPeekService(result);
            Assert.assertNull(result);

        } catch (Throwable e) {
            throw new RpcException("Failed to put " + url + " to redis . cause: " + e.getMessage(), e);
        } finally {
            redisServiceStore.pool.close();
        }
    }
}
