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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.metadata.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.store.redis.support.RedisUrlUtils;
import org.apache.dubbo.metadata.support.AbstractMetadataReport;
import org.apache.dubbo.rpc.RpcException;
import redis.clients.jedis.*;

import java.util.Set;

/**
 * RedisMetadataReport
 */
public class RedisMetadataReport extends AbstractMetadataReport {

    private final static Logger logger = LoggerFactory.getLogger(RedisMetadataReport.class);

    JedisPool pool;

    JedisCluster cluster;

    public RedisMetadataReport(URL url) {
        super(url);
        if (isSingleton(url)) {
            pool = new JedisPool(parsePoolConfig(url), url.getHost(), url.getPort());
        } else {
            Set<HostAndPort> hostAndPortsSet = RedisUrlUtils.parseHostAndPorts(url);
            cluster = new JedisCluster(hostAndPortsSet, parsePoolConfig(url));
        }
    }

    private boolean isSingleton(URL url) {
        // if address contains split char ",", return false
        if (Constants.COMMA_SPLIT_PATTERN.matcher(url.getAddress()).find()) {
            return false;
        }
        return url.getParameter("cluster") == null || "false".equals(url.getParameter("cluster"));
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
        try {
            doStoreMetadata(metadataIdentifier, v);
        } catch (Throwable e) {
            logger.error("Failed to put " + metadataIdentifier + " to redis " + v + ", cause: " + e.getMessage(), e);
            throw new RpcException("Failed to put " + metadataIdentifier + " to redis " + v + ", cause: " + e.getMessage(), e);
        }
    }

    private void doStoreMetadata(MetadataIdentifier metadataIdentifier, String v) {
        if (cluster == null) {
            try (Jedis jedis = pool.getResource()) {
                jedis.set(metadataIdentifier.getIdentifierKey() + META_DATA_STORE_TAG, v);
            } catch (Throwable e) {
                throw e;
            }
        } else {
            cluster.set(metadataIdentifier.getIdentifierKey() + META_DATA_STORE_TAG, v);
        }
    }

    private JedisPoolConfig parsePoolConfig(URL url) {
        JedisPoolConfig jpc = new JedisPoolConfig();
        //FIXME should add some config
        return jpc;
    }



}
