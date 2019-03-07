/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.metadata.store.redis;

import com.google.gson.Gson;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.common.Constants.SYNC_REPORT_KEY;
import static org.apache.dubbo.metadata.store.MetadataReport.META_DATA_SOTRE_TAG;

/**
 * 2018/10/9
 */
public class RedisMetadataReportTest {
    RedisMetadataReport redisMetadataReport;
    RedisMetadataReport syncRedisMetadataReport;
    RedisServer redisServer;

    @BeforeEach
    public void constructor() throws IOException {
        int redisPort = NetUtils.getAvailablePort();
        this.redisServer = new RedisServer(redisPort);
        this.redisServer.start();
        URL registryUrl = URL.valueOf("redis://localhost:" + redisPort);
        redisMetadataReport = (RedisMetadataReport) new RedisMetadataReportFactory().createMetadataReport(registryUrl);
        URL asyncRegistryUrl = URL.valueOf("redis://localhost:" + redisPort + "?" + SYNC_REPORT_KEY + "=true");
        syncRedisMetadataReport = (RedisMetadataReport) new RedisMetadataReportFactory().createMetadataReport(registryUrl);
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.redisServer.stop();
    }


    @Test
    public void testAsyncStoreProvider() throws ClassNotFoundException {
        testStoreProvider(redisMetadataReport, "1.0.0.redis.md.p1", 3000);
    }

    @Test
    public void testSyncStoreProvider() throws ClassNotFoundException {
        testStoreProvider(syncRedisMetadataReport, "1.0.0.redis.md.p2", 3);
    }

    private void testStoreProvider(RedisMetadataReport redisMetadataReport, String version, long moreTime) throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.store.redis.RedisMetadata4TstService";
        String group = null;
        String application = "vic.redis.md";
        MetadataIdentifier providerMetadataIdentifier = storePrivider(redisMetadataReport, interfaceName, version, group, application);
        Jedis jedis = null;
        try {
            jedis = redisMetadataReport.pool.getResource();
            String keyTmp = providerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY) + META_DATA_SOTRE_TAG;
            String value = jedis.get(keyTmp);
            if (value == null) {
                Thread.sleep(moreTime);
                value = jedis.get(keyTmp);
            }

            Assertions.assertNotNull(value);

            Gson gson = new Gson();
            FullServiceDefinition fullServiceDefinition = gson.fromJson(value, FullServiceDefinition.class);
            Assertions.assertEquals(fullServiceDefinition.getParameters().get("paramTest"), "redisTest");
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
    public void testAsyncStoreConsumer() throws ClassNotFoundException {
        testStoreConsumer(redisMetadataReport, "1.0.0.redis.md.c1", 3000);
    }

    @Test
    public void testSyncStoreConsumer() throws ClassNotFoundException {
        testStoreConsumer(syncRedisMetadataReport, "1.0.0.redis.md.c2", 3);
    }

    private void testStoreConsumer(RedisMetadataReport redisMetadataReport, String version, long moreTime) throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.store.redis.RedisMetadata4TstService";
        String group = null;
        String application = "vic.redis.md";
        MetadataIdentifier consumerMetadataIdentifier = storeConsumer(redisMetadataReport, interfaceName, version, group, application);
        Jedis jedis = null;
        try {
            jedis = redisMetadataReport.pool.getResource();
            String keyTmp = consumerMetadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY) + META_DATA_SOTRE_TAG;
            String value = jedis.get(keyTmp);
            if (value == null) {
                Thread.sleep(moreTime);
                value = jedis.get(keyTmp);
            }
            Assertions.assertEquals(value, "{\"paramConsumerTest\":\"redisCm\"}");
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
