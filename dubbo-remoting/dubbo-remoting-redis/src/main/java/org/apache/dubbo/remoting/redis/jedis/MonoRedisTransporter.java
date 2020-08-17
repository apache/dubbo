package org.apache.dubbo.remoting.redis.jedis;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.remoting.redis.RedisClient;
import org.apache.dubbo.remoting.redis.RedisTransporter;

public class MonoRedisTransporter implements RedisTransporter {
    @Override
    public RedisClient create(URL url) {
        return new MonoRedisClient(url);
    }
}
