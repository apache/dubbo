package org.apache.dubbo.remoting.redis;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

@SPI("mono")
public interface RedisTransporter {
    @Adaptive({"redis-mode"})
    RedisClient create(URL url);
}
