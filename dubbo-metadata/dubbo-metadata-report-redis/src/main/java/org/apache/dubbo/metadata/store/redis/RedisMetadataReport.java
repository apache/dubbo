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
import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ConcurrentHashMapUtils;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.MD5Utils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.report.identifier.BaseMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReport;
import org.apache.dubbo.rpc.RpcException;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.REGISTRY_ZOOKEEPER_EXCEPTION;
import static org.apache.dubbo.common.constants.LoggerCodeConstants.TRANSPORT_FAILED_RESPONSE;
import static org.apache.dubbo.metadata.MetadataConstants.META_DATA_STORE_TAG;
import static org.apache.dubbo.metadata.ServiceNameMapping.DEFAULT_MAPPING_GROUP;
import static org.apache.dubbo.metadata.ServiceNameMapping.getAppNames;

/**
 * RedisMetadataReport
 */
public class RedisMetadataReport extends AbstractMetadataReport {

    private static final String REDIS_DATABASE_KEY = "database";
    private static final ErrorTypeAwareLogger logger = LoggerFactory.getErrorTypeAwareLogger(RedisMetadataReport.class);

    // protected , for test
    protected JedisPool pool;
    private Set<HostAndPort> jedisClusterNodes;
    private int timeout;
    private String password;
    private final String root;
    private MD5Utils md5Utils = new MD5Utils();
    protected ConcurrentMap<String, MappingDataListener> listenerMap = new ConcurrentHashMap<>();
    private String luaScript="local key = KEYS[1];\n" +
        "local field = ARGV[1];\n" +
        "local oldValue = ARGV[2];\n" +
        "local newValue = ARGV[3];\n" +
        "local channel = ARGV[4];\n" +
        "local valueExists = redis.call(\"HEXISTS\", key, field);\n" +
        "if valueExists == 0 then\n" +
        "    redis.call(\"HSET\", key, field, newValue);\n" +
        "    redis.call(\"PUBLISH\", channel,newValue);\n" +
        "    return nil;\n" +
        "end;\n" +
        "local currentValue = redis.call(\"HGET\", key, field);\n" +
        "if currentValue == oldValue then\n" +
        "    redis.call(\"HSET\", key, field, newValue);\n" +
        "    redis.call(\"PUBLISH\", channel,newValue);\n" +
        "    return nil;\n" +
        "end;\n" +
        "return \"FAIL\";\n";


    public RedisMetadataReport(URL url) {
        super(url);
        timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
        password = url.getPassword();
        this.root = url.getGroup(DEFAULT_ROOT);
        if (url.getParameter(CLUSTER_KEY, false)) {
            jedisClusterNodes = new HashSet<>();
            List<URL> urls = url.getBackupUrls();
            for (URL tmpUrl : urls) {
                jedisClusterNodes.add(new HostAndPort(tmpUrl.getHost(), tmpUrl.getPort()));
            }
        } else {
            int database = url.getParameter(REDIS_DATABASE_KEY, 0);
            pool = new JedisPool(new JedisPoolConfig(), url.getHost(), url.getPort(), timeout, password, database);
        }
    }

    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        this.storeMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String value) {
        this.storeMetadata(consumerMetadataIdentifier, value);
    }

    @Override
    protected void doSaveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier, URL url) {
        this.storeMetadata(serviceMetadataIdentifier, URL.encode(url.toFullString()));
    }

    @Override
    protected void doRemoveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier) {
        this.deleteMetadata(serviceMetadataIdentifier);
    }

    @Override
    protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        String content = getMetadata(metadataIdentifier);
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(URL.decode(content)));
    }

    @Override
    protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urlListStr) {
        this.storeMetadata(subscriberMetadataIdentifier, urlListStr);
    }

    @Override
    protected String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        return this.getMetadata(subscriberMetadataIdentifier);
    }

    @Override
    public String getServiceDefinition(MetadataIdentifier metadataIdentifier) {
        return this.getMetadata(metadataIdentifier);
    }

    private void storeMetadata(BaseMetadataIdentifier metadataIdentifier, String v) {
        if (pool != null) {
            storeMetadataStandalone(metadataIdentifier, v);
        } else {
            storeMetadataInCluster(metadataIdentifier, v);
        }
    }

    private void storeMetadataInCluster(BaseMetadataIdentifier metadataIdentifier, String v) {
        try (JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
            jedisCluster.set(metadataIdentifier.getIdentifierKey() + META_DATA_STORE_TAG, v);
        } catch (Throwable e) {
            String msg = "Failed to put " + metadataIdentifier + " to redis cluster " + v + ", cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    private void storeMetadataStandalone(BaseMetadataIdentifier metadataIdentifier, String v) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), v);
        } catch (Throwable e) {
            String msg = "Failed to put " + metadataIdentifier + " to redis " + v + ", cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    private void deleteMetadata(BaseMetadataIdentifier metadataIdentifier) {
        if (pool != null) {
            deleteMetadataStandalone(metadataIdentifier);
        } else {
            deleteMetadataInCluster(metadataIdentifier);
        }
    }

    private void deleteMetadataInCluster(BaseMetadataIdentifier metadataIdentifier) {
        try (JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
            jedisCluster.del(metadataIdentifier.getIdentifierKey() + META_DATA_STORE_TAG);
        } catch (Throwable e) {
            String msg = "Failed to delete " + metadataIdentifier + " from redis cluster , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    private void deleteMetadataStandalone(BaseMetadataIdentifier metadataIdentifier) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
        } catch (Throwable e) {
            String msg = "Failed to delete " + metadataIdentifier + " from redis , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    private String getMetadata(BaseMetadataIdentifier metadataIdentifier) {
        if (pool != null) {
            return getMetadataStandalone(metadataIdentifier);
        } else {
            return getMetadataInCluster(metadataIdentifier);
        }
    }

    private String getMetadataInCluster(BaseMetadataIdentifier metadataIdentifier) {
        try (JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
            return jedisCluster.get(metadataIdentifier.getIdentifierKey() + META_DATA_STORE_TAG);
        } catch (Throwable e) {
            String msg = "Failed to get " + metadataIdentifier + " from redis cluster , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    private String getMetadataStandalone(BaseMetadataIdentifier metadataIdentifier) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.get(metadataIdentifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
        } catch (Throwable e) {
            String msg = "Failed to get " + metadataIdentifier + " from redis , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    @Override
    public boolean registerServiceAppMapping(String serviceInterface, String defaultMappingGroup, String newConfigContent, Object ticket) {
        try {
            if (null!= ticket && !(ticket instanceof String)) {
                throw new IllegalArgumentException("zookeeper publishConfigCas requires stat type ticket");
            }
            String pathKey = buildMappingKey(defaultMappingGroup);

            return storeMapping(pathKey, serviceInterface, newConfigContent,(String)ticket);
        } catch (Exception e) {
            logger.warn(REGISTRY_ZOOKEEPER_EXCEPTION, "", "", "redis publishConfigCas failed.", e);
            return false;
        }
    }


    private boolean storeMapping(String key, String field, String value,String ticket) {
        if (pool != null) {
            return storeMappingStandalone(key, field, value, ticket);
        } else {
            return storeMappingInCluster(key, field, value, ticket);
        }
    }

    private boolean storeMappingInCluster(String key, String field, String value,String ticket) {
        try (JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
            Object result =jedisCluster.eval(luaScript, 1, key, field, ticket==null?"":ticket, value,buildPubSubKey(field));
            return null==result;
        } catch (Throwable e) {
            String msg = "Failed to put " + key + ":" + field + " to redis " + value + ", cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    private boolean storeMappingStandalone(String key, String field, String value,String ticket) {
        try (Jedis jedis = pool.getResource()) {
            Object result = jedis.eval(luaScript, 1, key, field, ticket==null?"":ticket, value,buildPubSubKey(field));
            return null==result;
        } catch (Throwable e) {
            String msg = "Failed to put " + key + ":" + field + " to redis " + value + ", cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    private String buildMappingKey(String defaultMappingGroup) {
        return this.root + GROUP_CHAR_SEPARATOR + defaultMappingGroup;
    }

    private String buildPubSubKey(String serviceKey) {
        return buildMappingKey(DEFAULT_MAPPING_GROUP) + GROUP_CHAR_SEPARATOR + serviceKey;
    }

    @Override
    public ConfigItem getConfigItem(String serviceKey, String group) {
        String key = buildMappingKey(group);
        String content = getMappingData(key, serviceKey);

        return new ConfigItem(content, content);
    }

    private String getMappingData(String key, String field) {
        if (pool != null) {
            return getMappingDataStandalone(key, field);
        } else {
            return getMappingDataInCluster(key, field);
        }
    }

    private String getMappingDataInCluster(String key, String field) {
        try (JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
            return jedisCluster.hget(key, field);
        } catch (Throwable e) {
            String msg = "Failed to get " + key + ":" + field + " from redis cluster , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    private String getMappingDataStandalone(String key, String field) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hget(key, field);
        } catch (Throwable e) {
            String msg = "Failed to get " + key + ":" + field + " from redis , cause: " + e.getMessage();
            logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
            throw new RpcException(msg, e);
        }
    }

    @Override
    public void removeServiceAppMappingListener(String serviceKey, MappingListener listener) {
        if (null != listenerMap.get(serviceKey)) {
            MappingDataListener mappingDataListener=listenerMap.get(serviceKey);
            NotifySub notifySub=mappingDataListener.getNotifySub();
            notifySub.removeListener(listener);
            if(notifySub.isEmpty()){
                mappingDataListener.shutdown();
                listenerMap.remove(serviceKey,mappingDataListener);
            }
        }
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        if (null == listenerMap.get(serviceKey)) {
            NotifySub notifySub = new NotifySub(serviceKey);
            notifySub.addListener(listener);
            MappingDataListener dataListener = new MappingDataListener(buildPubSubKey(serviceKey), notifySub);
            ConcurrentHashMapUtils.computeIfAbsent(listenerMap, serviceKey
                , k -> dataListener);
            dataListener.start();
        }else{
            listenerMap.get(serviceKey).getNotifySub().addListener(listener);
        }
        return this.getServiceAppMapping(serviceKey, url);
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, URL url) {
        String key = buildMappingKey(DEFAULT_MAPPING_GROUP);
        return getAppNames(getMappingData(key, serviceKey));

    }

    @Override
    public MetadataInfo getAppMetadata(SubscriberMetadataIdentifier identifier, Map<String, String> instanceMetadata) {
        String content = this.getMetadata(identifier);
        return JsonUtils.toJavaObject(content, MetadataInfo.class);
    }

    @Override
    public void publishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        this.storeMetadata(identifier, metadataInfo.getContent());
    }

    @Override
    public void unPublishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        this.deleteMetadata(identifier);
    }

    private static class NotifySub extends JedisPubSub {

        private String serviceKey;
        private Set<MappingListener> listeners = new HashSet<>();

        public NotifySub(String serviceKey) {
            this.serviceKey = serviceKey;
        }

        public void addListener(MappingListener listener) {
            this.listeners.add(listener);
        }

        public void removeListener(MappingListener listener) {
            this.listeners.remove(listener);
        }

        public Boolean isEmpty(){
            return this.listeners.isEmpty();
        }

        @Override
        public void onMessage(String key, String msg) {
            logger.info("sub from redis " + key + " message:" + msg);
            MappingChangedEvent mappingChangedEvent = new MappingChangedEvent(serviceKey, getAppNames(msg));
            if(!listeners.isEmpty()){
                listeners.forEach(listener -> listener.onEvent(mappingChangedEvent));
            }

        }

        @Override
        public void onPMessage(String pattern, String key, String msg) {
            onMessage(key, msg);
        }

        @Override
        public void onPSubscribe(String pattern, int subscribedChannels) {
            super.onPSubscribe(pattern, subscribedChannels);
        }
    }

    private class MappingDataListener extends Thread {

        private String path;

        private volatile Jedis currentClient;

        private NotifySub notifySub;

        private volatile boolean running = true;

        public MappingDataListener(String path, NotifySub notifySub) {
            this.path = path;
            this.notifySub = notifySub;
        }


        public NotifySub getNotifySub() {
            return notifySub;
        }

        @Override
        public void run() {
            while (running) {
                if (pool != null) {
                    try (Jedis jedis = pool.getResource()) {
                        currentClient=jedis;
                        jedis.subscribe(notifySub, path);
                    }catch (Throwable e) {
                        String msg = "Failed to subscribe " + path + ", cause: " + e.getMessage();
                        logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
                        throw new RpcException(msg, e);
                    }
                } else {
                    try (JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig<>())) {
                        jedisCluster.subscribe(notifySub, path);
                    } catch (Throwable e) {
                        String msg = "Failed to subscribe " + path + ", cause: " + e.getMessage();
                        logger.error(TRANSPORT_FAILED_RESPONSE, "", "", msg, e);
                        throw new RpcException(msg, e);
                    }
                }
            }
        }

        public void shutdown() {
            try {
                running = false;
                notifySub.unsubscribe(path);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
        }
    }

}
