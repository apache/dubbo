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

package org.apache.dubbo.configcenter.support.redis;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.config.configcenter.TreePathDynamicConfiguration;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.remoting.redis.RedisClient;
import org.apache.dubbo.remoting.redis.jedis.ClusterRedisClient;
import org.apache.dubbo.remoting.redis.jedis.MonoRedisClient;
import org.apache.dubbo.remoting.redis.jedis.SentinelRedisClient;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.ANY_VALUE;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_REDIS;
import static org.apache.dubbo.common.constants.CommonConstants.MONO_REDIS;
import static org.apache.dubbo.common.constants.CommonConstants.REDIS_CLIENT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.SENTINEL_REDIS;
import static org.apache.dubbo.common.utils.StringUtils.SLASH;

/**
 * Redis DynamicConfiguration
 */
public class RedisDynamicConfiguration extends TreePathDynamicConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(RedisDynamicConfiguration.class);
    private static final String OK_RESPONSE = "OK";
    private static final Long DEL_OK = 1L;
    private static final long DEFAULT_CHECK_CONFIG_INTERVAL = 16000L;
    private static final String CHECK_CONFIG_INTERVAL = "redus-check-config-interval";
    /**
     * Store the mapping of pathKey to hash of value
     */
    private ConcurrentMap<String, HashHolder> configHashes = new ConcurrentHashMap<>();
    /**
     * Store the mapping of pathKey to configuration listener
     */
    private ConcurrentMap<String, ConfigurationListener> configListeners = new ConcurrentHashMap<>();
    private RedisClient redisClient;
    /**
     * Thread pool to poll the configuration
     */
    private ScheduledExecutorService configCheckExecutor;

    public RedisDynamicConfiguration(URL url) {
        super(url);
        String type = url.getParameter(REDIS_CLIENT_KEY, MONO_REDIS);
        if (SENTINEL_REDIS.equals(type)) {
            redisClient = new SentinelRedisClient(url);
        } else if (CLUSTER_REDIS.equals(type)) {
            redisClient = new ClusterRedisClient(url);
        } else {
            redisClient = new MonoRedisClient(url);
        }
        long checkPassInterval = url.getParameter(CHECK_CONFIG_INTERVAL, DEFAULT_CHECK_CONFIG_INTERVAL);
        configCheckExecutor = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("Ttl-Redis-Check-Executor", true));
        configCheckExecutor.scheduleAtFixedRate(this::checkConfig, checkPassInterval / 8,
                checkPassInterval / 8, TimeUnit.MILLISECONDS);
    }

    @Override
    protected boolean doPublishConfig(String pathKey, String content) throws Exception {
        return OK_RESPONSE.equals(redisClient.set(pathKey, content));
    }

    @Override
    protected String doGetConfig(String pathKey) throws Exception {
        return redisClient.get(pathKey);
    }

    @Override
    protected boolean doRemoveConfig(String pathKey) throws Exception {
        redisClient.del(pathKey);
        return true;
    }

    @Override
    protected Collection<String> doGetConfigKeys(String groupPath) {
        if (!groupPath.endsWith(ANY_VALUE)) {
            if (!groupPath.endsWith(SLASH)) {
                groupPath = groupPath + SLASH;
            }
            groupPath = groupPath + ANY_VALUE;
        }
        return redisClient.scan(groupPath);
    }

    @Override
    protected void doAddListener(String pathKey, ConfigurationListener listener) {
        logger.info("register listener " + listener.getClass() + " for config with key: " + pathKey);
        String value = redisClient.get(pathKey);
        if (value == null) {
            configHashes.put(pathKey, HashHolder.getNullHash());
        } else {
            configHashes.put(pathKey, new HashHolder(value.hashCode()));
        }
        configListeners.put(pathKey, listener);
    }

    @Override
    protected void doRemoveListener(String pathKey, ConfigurationListener listener) {
        logger.info("unregister listener " + listener.getClass() + " for config with key: " + pathKey);
        configHashes.remove(pathKey);
        configListeners.remove(pathKey);
    }

    @Override
    protected void doClose() throws Exception {
        configHashes.clear();
        configListeners.clear();
        configCheckExecutor.shutdownNow();
        redisClient.close();
    }

    private void checkConfig() {
        for (Map.Entry<String, HashHolder> entry : configHashes.entrySet()) {
            String value = redisClient.get(entry.getKey());
            if (value != null) {
                Integer hash = Integer.valueOf(value.hashCode());
                Integer originHash = entry.getValue().getHash();
                if (!hash.equals(originHash)) {
                    configHashes.put(entry.getKey(), new HashHolder(hash));
                    ConfigurationListener listener = configListeners.get(entry.getKey());
                    listener.process(new ConfigChangedEvent(entry.getKey(), getGroup(), value, ConfigChangeType.MODIFIED));
                }
            }
        }
    }

    /**
     * Holder of hash to avoid exception when put null value into ConcurrentHashMap
     */
    private static class HashHolder {
        private static HashHolder NULL_HASH = new HashHolder(null);

        private Integer hash;

        public HashHolder(Integer hash) {
            this.hash = hash;
        }

        public Integer getHash() {
            return hash;
        }

        public static HashHolder getNullHash() {
            return NULL_HASH;
        }
    }
}
