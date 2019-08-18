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
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.support.AbstractMetadataReport;
import org.apache.dubbo.rpc.RpcException;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.metadata.identifier.MetadataIdentifier.META_DATA_STORE_TAG;

/**
 * RedisMetadataReport
 */
public class RedisMetadataReport extends AbstractMetadataReport {

    private final static String REDIS_DATABASE_KEY = "database";
    private final static Logger logger = LoggerFactory.getLogger(RedisMetadataReport.class);

    JedisPool pool;
    Set<HostAndPort> jedisClusterNodes;
    private int timeout;
    private String password;


    public RedisMetadataReport(URL url) {
        super(url);
        timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT);
        password = url.getPassword();
        if (url.getParameter(CLUSTER_KEY, false)) {
            jedisClusterNodes = new HashSet<HostAndPort>();
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

    private void storeMetadata(MetadataIdentifier metadataIdentifier, String v) {
        if (pool != null) {
            storeMetadataStandalone(metadataIdentifier, v);
        } else {
            storeMetadataInCluster(metadataIdentifier, v);
        }
    }

    private void storeMetadataInCluster(MetadataIdentifier metadataIdentifier, String v) {
        try (JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes, timeout, timeout, 2, password, new GenericObjectPoolConfig())) {
            jedisCluster.set(metadataIdentifier.getIdentifierKey() + META_DATA_STORE_TAG, v);
        } catch (Throwable e) {
            logger.error("Failed to put " + metadataIdentifier + " to redis cluster " + v + ", cause: " + e.getMessage(), e);
            throw new RpcException("Failed to put " + metadataIdentifier + " to redis cluster " + v + ", cause: " + e.getMessage(), e);
        }
    }

    private void storeMetadataStandalone(MetadataIdentifier metadataIdentifier, String v) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(metadataIdentifier.getUniqueKey(MetadataIdentifier.KeyTypeEnum.UNIQUE_KEY), v);
        } catch (Throwable e) {
            logger.error("Failed to put " + metadataIdentifier + " to redis " + v + ", cause: " + e.getMessage(), e);
            throw new RpcException("Failed to put " + metadataIdentifier + " to redis " + v + ", cause: " + e.getMessage(), e);
        }
    }

}
