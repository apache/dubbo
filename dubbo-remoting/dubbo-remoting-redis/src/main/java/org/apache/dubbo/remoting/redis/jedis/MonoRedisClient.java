package org.apache.dubbo.remoting.redis.jedis;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.redis.RedisClient;
import org.apache.dubbo.remoting.redis.support.AbstractRedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_TIMEOUT;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_KEY;

public class MonoRedisClient extends AbstractRedisClient implements RedisClient {
    private static final Logger logger = LoggerFactory.getLogger(MonoRedisClient.class);

    private static final String START_CURSOR = "0";

    private JedisPool jedisPool;

    public MonoRedisClient(URL url) {
        super(url);
        jedisPool = new JedisPool(getConfig(), url.getHost(), url.getPort(),
                url.getParameter(TIMEOUT_KEY, DEFAULT_TIMEOUT), url.getPassword());
    }

    @Override
    public Long hset(String key, String field, String value) {
        Jedis jedis = jedisPool.getResource();
        Long result = jedis.hset(key, field, value);
        jedis.close();
        return result;
    }

    @Override
    public Long publish(String channel, String message) {
        Jedis jedis = jedisPool.getResource();
        Long result = jedis.publish(channel, message);
        jedis.close();
        return result;
    }

//    @Override
//    public void clean(String pattern) {
//        Set<String> keys = scan(pattern);
//        Jedis jedis = jedisPool.getResource();
//        if (CollectionUtils.isNotEmpty(keys)) {
//            for (String key : keys) {
//                Map<String, String> values = jedis.hgetAll(key);
//                if (CollectionUtils.isNotEmptyMap(values)) {
//                    boolean delete = false;
//                    long now = System.currentTimeMillis();
//                    for (Map.Entry<String, String> entry : values.entrySet()) {
//                        URL url = URL.valueOf(entry.getKey());
//                        if (url.getParameter(DYNAMIC_KEY, true)) {
//                            long expire = Long.parseLong(entry.getValue());
//                            if (expire < now) {
//                                jedis.hdel(key, entry.getKey());
//                                delete = true;
//                                if (logger.isWarnEnabled()) {
//                                    logger.warn("Delete expired key: " + key + " -> value: " + entry.getKey() + ", expire: " + new Date(expire) + ", now: " + new Date(now));
//                                }
//                            }
//                        }
//                    }
//                    if (delete) {
//                        jedis.publish(key, UNREGISTER);
//                    }
//                }
//            }
//        }
//    }

    @Override
    public boolean isConnected() {
        Jedis jedis = jedisPool.getResource();
        boolean connected = jedis.isConnected();
        jedis.close();
        return connected;
    }

    @Override
    public void destroy() {
        jedisPool.close();
    }

    @Override
    public Long hdel(String key, String... fields) {
        Jedis jedis = jedisPool.getResource();
        Long result = jedis.hdel(key, fields);
        jedis.close();
        return result;
    }

    @Override
    public Set<String> scan(String pattern) {
        Jedis jedis = jedisPool.getResource();
        Set<String> result = super.scan(jedis, pattern);
        jedis.close();
        return result;
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        Jedis jedis = jedisPool.getResource();
        Map<String, String> result = jedis.hgetAll(key);
        jedis.close();
        return result;
    }

    @Override
    public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
        Jedis jedis = jedisPool.getResource();
        jedis.psubscribe(jedisPubSub, patterns);
        jedis.close();
    }

    @Override
    public void disconnect() {
        jedisPool.close();
    }

    @Override
    public void close() {
        jedisPool.close();
    }


}
