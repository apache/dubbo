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
package org.apache.dubbo.remoting.redis.jedis;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.redis.RedisClient;
import org.apache.dubbo.remoting.redis.support.AbstractRedisClient;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.COLON_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;

public class ClusterRedisClient extends AbstractRedisClient implements RedisClient {
    private static final Logger logger = LoggerFactory.getLogger(ClusterRedisClient.class);

    private static final int DEFAULT_TIMEOUT = 2000;

    private static final int DEFAULT_SO_TIMEOUT = 2000;

    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private JedisCluster jedisCluster;

    public ClusterRedisClient(URL url) {
        super(url);
        Set<HostAndPort> nodes = getNodes(url);
        jedisCluster = new JedisCluster(nodes, url.getParameter("connection.timeout", DEFAULT_TIMEOUT),
                url.getParameter("so.timeout", DEFAULT_SO_TIMEOUT), url.getParameter("max.attempts", DEFAULT_MAX_ATTEMPTS),
                url.getPassword(), getConfig());
    }

    @Override
    public Long hset(String key, String field, String value) {
        return jedisCluster.hset(key, field, value);
    }

    @Override
    public Long publish(String channel, String message) {
        return jedisCluster.publish(channel, message);
    }

    @Override
    public boolean isConnected() {
        Map<String, JedisPool> poolMap = jedisCluster.getClusterNodes();
        for (JedisPool jedisPool : poolMap.values()) {
            Jedis jedis = jedisPool.getResource();
            if (jedis.isConnected()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        jedisCluster.close();
    }

    @Override
    public Long hdel(String key, String... fields) {
        return jedisCluster.hdel(key, fields);
    }

    @Override
    public Set<String> scan(String pattern) {
        Map<String, JedisPool> nodes = jedisCluster.getClusterNodes();
        Set<String> result = new HashSet<>();
        for (JedisPool jedisPool : nodes.values()) {
            Jedis jedis = jedisPool.getResource();
            result.addAll(scan(jedis, pattern));
            jedis.close();
        }
        return result;
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        return jedisCluster.hgetAll(key);
    }

    @Override
    public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
        jedisCluster.psubscribe(jedisPubSub, patterns);
    }

    @Override
    public void disconnect() {
        jedisCluster.close();
    }

    @Override
    public void close() {
        jedisCluster.close();
    }

    private Set<HostAndPort> getNodes(URL url) {
        Set<HostAndPort> hostAndPorts = new HashSet<>();
        hostAndPorts.add(new HostAndPort(url.getHost(), url.getPort()));
        String backupAddresses = url.getBackupAddress(6379);
        String[] nodes = StringUtils.isEmpty(backupAddresses) ? new String[0] : COMMA_SPLIT_PATTERN.split(backupAddresses);
        if (nodes.length > 0) {
            for (String node : nodes) {
                String[] hostAndPort = COLON_SPLIT_PATTERN.split(node);
                hostAndPorts.add(new HostAndPort(hostAndPort[0], Integer.valueOf(hostAndPort[1])));
            }
        }
        return hostAndPorts;
    }
}
