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
package org.apache.dubbo.remoting.redis;

import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.Set;

public interface RedisClient {
    Long hset(String key, String field, String value);

    Long publish(String channel, String message);

//    void clean(String pattern);

    boolean isConnected();

    void destroy();

    Long hdel(final String key, final String... fields);

    Set<String> scan(String pattern);

    Map<String, String> hgetAll(String key);

    void psubscribe(final JedisPubSub jedisPubSub, final String... patterns);

    void disconnect();

    void close();
}
