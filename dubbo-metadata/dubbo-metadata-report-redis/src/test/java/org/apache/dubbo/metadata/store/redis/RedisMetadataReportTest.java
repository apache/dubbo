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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.definition.ServiceDefinitionBuilder;
import org.apache.dubbo.metadata.definition.model.FullServiceDefinition;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.rpc.RpcException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.embedded.RedisServer;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER_SIDE;
import static org.apache.dubbo.common.constants.CommonConstants.SYNC_REPORT_KEY;
import static org.apache.dubbo.metadata.ServiceNameMapping.DEFAULT_MAPPING_GROUP;
import static redis.embedded.RedisServer.newRedisServer;

@DisabledOnOs(OS.WINDOWS)
class RedisMetadataReportTest {

    private static final String REDIS_URL_TEMPLATE = "redis://%slocalhost:%d",
            REDIS_PASSWORD = "チェリー",
            REDIS_URL_AUTH_SECTION = "username:" + REDIS_PASSWORD + "@";

    RedisMetadataReport redisMetadataReport;
    RedisMetadataReport syncRedisMetadataReport;
    RedisServer redisServer;
    URL registryUrl;

    @BeforeEach
    public void constructor(final TestInfo testInfo) {
        final boolean usesAuthentication = usesAuthentication(testInfo);
        int redisPort = 0;
        IOException exception = null;

        for (int i = 0; i < 10; i++) {
            try {
                redisPort = NetUtils.getAvailablePort(30000 + new Random().nextInt(10000));
                redisServer = newRedisServer()
                        .port(redisPort)
                        // set maxheap to fix Windows error 0x70 while starting redis
                        // .settingIf(SystemUtils.IS_OS_WINDOWS, "maxheap 128mb")
                        .settingIf(usesAuthentication, "requirepass " + REDIS_PASSWORD)
                        .build();
                this.redisServer.start();
                exception = null;
            } catch (IOException e) {
                e.printStackTrace();
                exception = e;
            }
            if (exception == null) {
                break;
            }
        }

        Assertions.assertNull(exception);
        registryUrl = newRedisUrl(usesAuthentication, redisPort);
        redisMetadataReport = (RedisMetadataReport) new RedisMetadataReportFactory().createMetadataReport(registryUrl);
        URL syncRegistryUrl = registryUrl.addParameter(SYNC_REPORT_KEY, "true");
        syncRedisMetadataReport =
                (RedisMetadataReport) new RedisMetadataReportFactory().createMetadataReport(syncRegistryUrl);
    }

    private static boolean usesAuthentication(final TestInfo testInfo) {
        final String methodName = testInfo.getTestMethod().get().getName();
        return "testAuthRedisMetadata".equals(methodName) || "testWrongAuthRedisMetadata".equals(methodName);
    }

    private static URL newRedisUrl(final boolean usesAuthentication, final int redisPort) {
        final String urlAuthSection = usesAuthentication ? REDIS_URL_AUTH_SECTION : "";
        return URL.valueOf(String.format(REDIS_URL_TEMPLATE, urlAuthSection, redisPort));
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.redisServer.stop();
    }

    @Test
    void testAsyncStoreProvider() throws ClassNotFoundException {
        testStoreProvider(redisMetadataReport, "1.0.0.redis.md.p1", 3000);
    }

    @Test
    void testSyncStoreProvider() throws ClassNotFoundException {
        testStoreProvider(syncRedisMetadataReport, "1.0.0.redis.md.p2", 3);
    }

    private void testStoreProvider(RedisMetadataReport redisMetadataReport, String version, long moreTime)
            throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.store.redis.RedisMetadata4TstService";
        String group = null;
        String application = "vic.redis.md";
        MetadataIdentifier providerMetadataIdentifier =
                storePrivider(redisMetadataReport, interfaceName, version, group, application);
        Jedis jedis = null;
        try {
            jedis = redisMetadataReport.pool.getResource();
            String keyTmp = providerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY);
            String value = jedis.get(keyTmp);
            if (value == null) {
                Thread.sleep(moreTime);
                value = jedis.get(keyTmp);
            }

            Assertions.assertNotNull(value);

            FullServiceDefinition fullServiceDefinition = JsonUtils.toJavaObject(value, FullServiceDefinition.class);
            Assertions.assertEquals(fullServiceDefinition.getParameters().get("paramTest"), "redisTest");
        } catch (Throwable e) {
            throw new RpcException("Failed to put to redis . cause: " + e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.del(providerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
            }
            redisMetadataReport.pool.close();
        }
    }

    @Test
    void testAsyncStoreConsumer() throws ClassNotFoundException {
        testStoreConsumer(redisMetadataReport, "1.0.0.redis.md.c1", 3000);
    }

    @Test
    void testSyncStoreConsumer() throws ClassNotFoundException {
        testStoreConsumer(syncRedisMetadataReport, "1.0.0.redis.md.c2", 3);
    }

    private void testStoreConsumer(RedisMetadataReport redisMetadataReport, String version, long moreTime)
            throws ClassNotFoundException {
        String interfaceName = "org.apache.dubbo.metadata.store.redis.RedisMetadata4TstService";
        String group = null;
        String application = "vic.redis.md";
        MetadataIdentifier consumerMetadataIdentifier =
                storeConsumer(redisMetadataReport, interfaceName, version, group, application);
        Jedis jedis = null;
        try {
            jedis = redisMetadataReport.pool.getResource();
            String keyTmp = consumerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY);
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
                jedis.del(consumerMetadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
            }
            redisMetadataReport.pool.close();
        }
    }

    private MetadataIdentifier storePrivider(
            RedisMetadataReport redisMetadataReport,
            String interfaceName,
            String version,
            String group,
            String application)
            throws ClassNotFoundException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName
                + "?paramTest=redisTest&version=" + version + "&application=" + application
                + (group == null ? "" : "&group=" + group));

        MetadataIdentifier providerMetadataIdentifier =
                new MetadataIdentifier(interfaceName, version, group, PROVIDER_SIDE, application);
        Class interfaceClass = Class.forName(interfaceName);
        FullServiceDefinition fullServiceDefinition =
                ServiceDefinitionBuilder.buildFullDefinition(interfaceClass, url.getParameters());

        redisMetadataReport.storeProviderMetadata(providerMetadataIdentifier, fullServiceDefinition);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return providerMetadataIdentifier;
    }

    private MetadataIdentifier storeConsumer(
            RedisMetadataReport redisMetadataReport,
            String interfaceName,
            String version,
            String group,
            String application)
            throws ClassNotFoundException {
        URL url = URL.valueOf("xxx://" + NetUtils.getLocalAddress().getHostName() + ":4444/" + interfaceName
                + "?version=" + version + "&application=" + application + (group == null ? "" : "&group=" + group));

        MetadataIdentifier consumerMetadataIdentifier =
                new MetadataIdentifier(interfaceName, version, group, CONSUMER_SIDE, application);
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

    @Test
    void testAuthRedisMetadata() throws ClassNotFoundException {
        testStoreProvider(redisMetadataReport, "1.0.0.redis.md.p1", 3000);
    }

    @Test
    void testWrongAuthRedisMetadata() throws ClassNotFoundException {
        redisMetadataReport = (RedisMetadataReport) new RedisMetadataReportFactory().createMetadataReport(registryUrl);
        try {
            testStoreProvider(redisMetadataReport, "1.0.0.redis.md.p1", 3000);
        } catch (RpcException e) {
            if (e.getCause() instanceof JedisConnectionException
                    && e.getCause().getCause() instanceof JedisDataException) {
                Assertions.assertEquals(
                        "WRONGPASS invalid username-password pair or user is disabled.",
                        e.getCause().getCause().getMessage());
            } else {
                Assertions.fail("no invalid password exception!");
            }
        }
    }

    @Test
    void testRegisterServiceAppMapping() throws InterruptedException {
        String serviceKey1 = "org.apache.dubbo.metadata.store.redis.RedisMetadata4TstService";
        String serviceKey2 = "org.apache.dubbo.metadata.store.redis.RedisMetadata4TstService2";

        String appNames1 = "test1";
        String appNames2 = "test1,test2";
        CountDownLatch latch = new CountDownLatch(2);
        CountDownLatch latch2 = new CountDownLatch(2);

        MappingListener mappingListener = new MappingListener() {
            @Override
            public void onEvent(MappingChangedEvent event) {
                Set<String> apps = event.getApps();
                if (apps.size() == 1) {
                    Assertions.assertTrue(apps.contains("test1"));
                } else {
                    Assertions.assertTrue(apps.contains("test1"));
                    Assertions.assertTrue(apps.contains("test2"));
                }
                if (serviceKey1.equals(event.getServiceKey())) {
                    latch.countDown();
                } else if (serviceKey2.equals(event.getServiceKey())) {
                    latch2.countDown();
                }
            }

            @Override
            public void stop() {}
        };

        Set<String> serviceAppMapping =
                redisMetadataReport.getServiceAppMapping(serviceKey1, mappingListener, registryUrl);

        Assertions.assertTrue(serviceAppMapping.isEmpty());

        ConfigItem configItem = redisMetadataReport.getConfigItem(serviceKey1, DEFAULT_MAPPING_GROUP);

        redisMetadataReport.registerServiceAppMapping(
                serviceKey1, DEFAULT_MAPPING_GROUP, appNames1, configItem.getTicket());
        configItem = redisMetadataReport.getConfigItem(serviceKey1, DEFAULT_MAPPING_GROUP);

        redisMetadataReport.registerServiceAppMapping(
                serviceKey1, DEFAULT_MAPPING_GROUP, appNames2, configItem.getTicket());

        latch.await();

        serviceAppMapping = redisMetadataReport.getServiceAppMapping(serviceKey2, mappingListener, registryUrl);

        Assertions.assertTrue(serviceAppMapping.isEmpty());

        configItem = redisMetadataReport.getConfigItem(serviceKey2, DEFAULT_MAPPING_GROUP);

        redisMetadataReport.registerServiceAppMapping(
                serviceKey2, DEFAULT_MAPPING_GROUP, appNames1, configItem.getTicket());
        configItem = redisMetadataReport.getConfigItem(serviceKey2, DEFAULT_MAPPING_GROUP);
        redisMetadataReport.registerServiceAppMapping(
                serviceKey2, DEFAULT_MAPPING_GROUP, appNames2, configItem.getTicket());

        latch2.await();
        RedisMetadataReport.MappingDataListener mappingDataListener = redisMetadataReport.getMappingDataListener();
        Assertions.assertTrue(mappingDataListener.running);
        Assertions.assertTrue(!mappingDataListener.getNotifySub().isEmpty());

        redisMetadataReport.removeServiceAppMappingListener(serviceKey1, mappingListener);
        Assertions.assertTrue(mappingDataListener.running);
        Assertions.assertTrue(!mappingDataListener.getNotifySub().isEmpty());
        redisMetadataReport.removeServiceAppMappingListener(serviceKey2, mappingListener);
        Assertions.assertTrue(!mappingDataListener.running);
        Assertions.assertTrue(mappingDataListener.getNotifySub().isEmpty());
    }

    @Test
    void testAppMetadata() {
        String serviceKey = "org.apache.dubbo.metadata.store.redis.RedisMetadata4TstService";
        String appName = "demo";
        URL url = URL.valueOf("test://127.0.0.1:8888/" + serviceKey);

        MetadataInfo metadataInfo = new MetadataInfo(appName);
        metadataInfo.addService(url);
        SubscriberMetadataIdentifier identifier =
                new SubscriberMetadataIdentifier(appName, metadataInfo.calAndGetRevision());
        MetadataInfo appMetadata = redisMetadataReport.getAppMetadata(identifier, Collections.emptyMap());
        Assertions.assertNull(appMetadata);

        redisMetadataReport.publishAppMetadata(identifier, metadataInfo);
        appMetadata = redisMetadataReport.getAppMetadata(identifier, Collections.emptyMap());
        Assertions.assertNotNull(appMetadata);
        Assertions.assertEquals(appMetadata.toFullString(), metadataInfo.toFullString());
        redisMetadataReport.unPublishAppMetadata(identifier, metadataInfo);
        appMetadata = redisMetadataReport.getAppMetadata(identifier, Collections.emptyMap());
        Assertions.assertNull(appMetadata);
    }
}
