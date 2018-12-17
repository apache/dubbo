package org.apache.dubbo.metadata.store.redis;

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.rpc.RpcException;

import com.google.gson.Gson;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.metadata.store.MetadataReport.META_DATA_SOTRE_TAG;

/**
 * 2018/10/9
 */
public class RedisMetadataReportTest {
    RedisMetadataReport redisMetadataReport;
    RedisServer redisServer;

    @Before
    public void constructor() throws IOException {
        int redisPort = NetUtils.getAvailablePort();
        this.redisServer = new RedisServer(redisPort);
        this.redisServer.start();
        URL registryUrl = URL.valueOf("redis://localhost:" + redisPort);
        redisMetadataReport = (RedisMetadataReport) new RedisMetadataReportFactory().createMetadataReport(registryUrl);
    }

    @After
    public void tearDown() throws Exception {
        this.redisServer.stop();
    }


    @Test
    public void testStoreProvider() throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.store.redis.RedisMetadata4TstService";
        String version = "1.0.0.redis.md";
        String group = null;
        String application = "vic.redis.md";
        MetadataIdentifier providerMetadataIdentifier = storePrivider(redisMetadataReport, interfaceName, version, group, application);
        Jedis jedis = null;
        try {
            jedis = redisMetadataReport.pool.getResource();
            String value = jedis.get(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY) + META_DATA_SOTRE_TAG);
            Assert.assertNotNull(value);

            Gson gson = new Gson();
            FullServiceDefinition fullServiceDefinition = gson.fromJson(value, FullServiceDefinition.class);
            Assert.assertEquals(fullServiceDefinition.getParameters().get("paramTest"), "redisTest");
        } catch (Throwable e) {
            throw new RpcException("Failed to put to redis . cause: " + e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.del(providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY) + META_DATA_SOTRE_TAG);
            }
            redisMetadataReport.pool.close();
        }
    }

    @Test
    public void testStoreConsumer() throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.store.redis.RedisMetadata4TstService";
        String version = "1.0.0.redis.md";
        String group = null;
        String application = "vic.redis.md";
        MetadataIdentifier consumerMetadataIdentifier = storeConsumer(redisMetadataReport, interfaceName, version, group, application);
        Jedis jedis = null;
        try {
            jedis = redisMetadataReport.pool.getResource();
            String value = jedis.get(consumerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY) + META_DATA_SOTRE_TAG);
            Assert.assertEquals(value, "{\"paramConsumerTest\":\"redisCm\"}");
        } catch (Throwable e) {
            throw new RpcException("Failed to put to redis . cause: " + e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.del(consumerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY) + META_DATA_SOTRE_TAG);
            }
            redisMetadataReport.pool.close();
        }
    }

    private MetadataIdentifier storePrivider(RedisMetadataReport redisMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?paramTest=redisTest&version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group));

        MetadataIdentifier providerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, Constants.PROVIDER_SIDE, application);
        Class interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        redisMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return providerMetadataIdentifier;
    }

    private MetadataIdentifier storeConsumer(RedisMetadataReport redisMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group));

        MetadataIdentifier consumerMetadataIdentifier = new MetadataIdentifier(interfaceName, version, group, Constants.CONSUMER_SIDE, application);
        Class interfaceClass = Class.forName(interfaceName);

        Map<String, String> tmp = new HashMap<>();
        tmp.put("paramConsumerTest", "redisCm");
        redisMetadataReport.storeConsumerMetadata(consumerMetadataIdentifier, tmp);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return consumerMetadataIdentifier;
    }

}
