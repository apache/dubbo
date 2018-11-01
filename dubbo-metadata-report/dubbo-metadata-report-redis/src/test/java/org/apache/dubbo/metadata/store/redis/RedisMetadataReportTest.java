package org.apache.dubbo.metadata.store.redis;

import com.google.gson.Gson;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.ConsumerMetadataIdentifier;
import org.apache.dubbo.metadata.identifier.ProviderMetadataIdentifier;
import org.apache.dubbo.rpc.RpcException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;

import java.io.IOException;

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
        ProviderMetadataIdentifier providerMetadataIdentifier = storePrivider(redisMetadataReport, interfaceName, version, group, application);
        Jedis jedis = null;
        try {
            jedis = redisMetadataReport.pool.getResource();
            String value = jedis.get(providerMetadataIdentifier.getIdentifierKey() + META_DATA_SOTRE_TAG);
            Assert.assertNotNull(value);

            Gson gson = new Gson();
            FullServiceDefinition fullServiceDefinition = gson.fromJson(value, FullServiceDefinition.class);
            Assert.assertEquals(fullServiceDefinition.getParameters().get("paramTest"), "redisTest");
        } catch (Throwable e) {
            throw new RpcException("Failed to put to redis . cause: " + e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.del(providerMetadataIdentifier.getIdentifierKey() + META_DATA_SOTRE_TAG);
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
        ConsumerMetadataIdentifier consumerMetadataIdentifier = storeConsumer(redisMetadataReport, interfaceName, version, group, application);
        Jedis jedis = null;
        try {
            jedis = redisMetadataReport.pool.getResource();
            String value = jedis.get(consumerMetadataIdentifier.getIdentifierKey() + META_DATA_SOTRE_TAG);
            Assert.assertEquals(value, "paramConsumerTest=redisCm");
        } catch (Throwable e) {
            throw new RpcException("Failed to put to redis . cause: " + e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.del(consumerMetadataIdentifier.getIdentifierKey() + META_DATA_SOTRE_TAG);
            }
            redisMetadataReport.pool.close();
        }
    }

    private ProviderMetadataIdentifier storePrivider(RedisMetadataReport redisMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?paramTest=redisTest&version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group));

        ProviderMetadataIdentifier providerMetadataIdentifier = new ProviderMetadataIdentifier(interfaceName, version, group);
        Class interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition = ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        redisMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);

        return providerMetadataIdentifier;
    }

    private ConsumerMetadataIdentifier storeConsumer(RedisMetadataReport redisMetadataReport, String interfaceName, String version, String group, String application) throws ClassNotFoundException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName + "?version=" + version + "&application="
                + application + (group == null ? "" : "&group=" + group));

        ConsumerMetadataIdentifier consumerMetadataIdentifier = new ConsumerMetadataIdentifier(interfaceName, version, group, application);
        Class interfaceClass = Class.forName(interfaceName);

        redisMetadataReport.storeConsumerMetadata(consumerMetadataIdentifier, "paramConsumerTest=redisCm");

        return consumerMetadataIdentifier;
    }

}
