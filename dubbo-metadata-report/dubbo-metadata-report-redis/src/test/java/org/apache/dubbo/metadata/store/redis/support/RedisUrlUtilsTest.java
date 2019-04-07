package org.apache.dubbo.metadata.store.redis.support;

import org.apache.dubbo.common.URL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.HostAndPort;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RedisUrlUtilsTest {

    @Test
    void parseHostAndPorts() {
        URL url = URL.valueOf("redis://127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381,127.0.0.1:6382,127.0.0.1:6383,127.0.0.1:6384");
        Set<HostAndPort> hostAndPorts = RedisUrlUtils.parseHostAndPorts(url);

        Assertions.assertIterableEquals(hostAndPorts, Arrays.asList(
                new HostAndPort("127.0.0.1", 6379),
                new HostAndPort("127.0.0.1", 6380),
                new HostAndPort("127.0.0.1", 6381),
                new HostAndPort("127.0.0.1", 6382),
                new HostAndPort("127.0.0.1", 6383),
                new HostAndPort("127.0.0.1", 6384)
        ));
    }
}