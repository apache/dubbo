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