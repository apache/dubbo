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
import org.apache.dubbo.common.constants.RemotingConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.remoting.redis.RedisClient;
import org.apache.dubbo.remoting.redis.support.AbstractRedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.JedisSentinelPool;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SentinelRedisClient extends AbstractRedisClient implements RedisClient {
    private static final Logger logger = LoggerFactory.getLogger(SentinelRedisClient.class);

    private JedisSentinelPool sentinelPool;

    public SentinelRedisClient(URL url) {
        super(url);
        String masterName = url.getParameter("master.name", "Sentinel-master");
        String address = (new StringBuilder()).append(url.getAddress()).toString();
        String[] backupAddresses = url.getParameter(RemotingConstants.BACKUP_KEY, new String[0]);
        if (backupAddresses.length == 0) {
            throw new IllegalStateException("Sentinel addresses can not be empty");
        }
        Set<String> sentinels = new HashSet<>(Arrays.asList(backupAddresses));
        sentinels.add(address);
        sentinelPool = new JedisSentinelPool(masterName, sentinels, getConfig(), url.getPassword());
    }

    @Override
    public Long hset(String key, String field, String value) {
        Jedis jedis = sentinelPool.getResource();
        Long result = jedis.hset(key, field, value);
        jedis.close();
        return result;
    }

    @Override
    public Long publish(String channel, String message) {
        Jedis jedis = sentinelPool.getResource();
        Long result = jedis.publish(channel, message);
        jedis.close();
        return result;
    }

    @Override
    public boolean isConnected() {
        Jedis jedis = sentinelPool.getResource();
        boolean result = jedis.isConnected();
        jedis.close();
        return result;
    }

    @Override
    public void destroy() {
        sentinelPool.close();
    }

    @Override
    public Long hdel(String key, String... fields) {
        Jedis jedis = sentinelPool.getResource();
        Long result = jedis.hdel(key, fields);
        jedis.close();
        return result;
    }

    @Override
    public Set<String> scan(String pattern) {
        Jedis jedis = sentinelPool.getResource();
        Set<String> result = scan(jedis, pattern);
        jedis.close();
        return result;
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        Jedis jedis = sentinelPool.getResource();
        Map<String, String> result = jedis.hgetAll(key);
        jedis.close();
        return result;
    }

    @Override
    public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
        Jedis jedis = sentinelPool.getResource();
        jedis.psubscribe(jedisPubSub, patterns);
        jedis.close();
    }

    @Override
    public void disconnect() {
        sentinelPool.close();
    }

    @Override
    public void close() {
        sentinelPool.close();
    }
}
