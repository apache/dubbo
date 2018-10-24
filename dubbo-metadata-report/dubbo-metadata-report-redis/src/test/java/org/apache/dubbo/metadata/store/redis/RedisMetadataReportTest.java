package org.apache.dubbo.metadata.store.redis;

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
 *  2018/10/9
 */
public class RedisMetadataReportTest {
    RedisMetadataReport redisServiceStore;
    RedisServer redisServer;

    @Before
    public void constructor() throws IOException {
        int redisPort = NetUtils.getAvailablePort();
        this.redisServer = new RedisServer(redisPort);
        this.redisServer.start();
        URL registryUrl = URL.valueOf("redis://localhost:" + redisPort);
        redisServiceStore = (RedisMetadataReport) new RedisMetadataReportFactory().createMetadataReport(registryUrl);
    }

    @After
    public void tearDown() throws Exception {
        this.redisServer.stop();
    }



    @Test
    public void testDoPut(){
        URL url = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4444/org.apache.dubbo.TestService?version=1.0.9&application=vicss&side=provider");
        redisServiceStore.doPut(url);

        try {
            Jedis jedis = redisServiceStore.pool.getResource();
            String value = jedis.get(redisServiceStore.getUrlKey(url));
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
        redisServiceStore.doPut(url);

        try {
            URL result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.TestService?version=1.0.9&application=vicww&side=provider");
            result = redisServiceStore.doPeek(result);
            Assert.assertFalse(result.getParameters().isEmpty());
            Assert.assertEquals(result.getParameter("version"),"1.0.9");
            Assert.assertEquals(result.getParameter("application"),"vicss");
            Assert.assertEquals(result.getParameter("side"),"provider");


            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.TestDD?version=1.0.9&application=vicss&side=provider");
            result = redisServiceStore.doPeek(result);
            Assert.assertNull(result);

            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.TestService?version=1.0.999&application=vicss&side=provider");
            result = redisServiceStore.doPeek(result);
            Assert.assertNull(result);

            result = URL.valueOf("dubbo://" + NetUtils.getLocalAddress().getHostName() + ":4667/org.apache.dubbo.TestService?version=1.0.9&application=vicss&side=consumer");
            result = redisServiceStore.doPeek(result);
            Assert.assertNull(result);

        } catch (Throwable e) {
            throw new RpcException("Failed to put " + url + " to redis . cause: " + e.getMessage(), e);
        } finally {
            redisServiceStore.pool.close();
        }
    }
}
