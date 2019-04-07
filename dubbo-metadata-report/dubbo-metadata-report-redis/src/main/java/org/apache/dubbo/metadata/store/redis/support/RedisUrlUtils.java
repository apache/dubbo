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

import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import redis.clients.jedis.HostAndPort;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * redis url helper
 *
 * @author wuhulala
 * @date 2019/4/7
 */
public class RedisUrlUtils {

    /**
     * parse url
     *
     * @param url url such as redis://127.0.0.1:6379,127.0.0.1:6380
     * @return such as return {HostAndPort("127.0.0.1",6379), HostAndPort("127.0.0.1",6380)}
     */
    public static Set<HostAndPort> parseHostAndPorts(URL url) {
        Set<HostAndPort> hostAndPortsSet = new LinkedHashSet<>(32);
        String hapStr = url.getAddress();
        if (StringUtils.isNotEmpty(hapStr)) {
            String[] nodes = Constants.COMMA_SPLIT_PATTERN.split(hapStr);
            for (String node : nodes) {
                int splitIndex = node.indexOf(":");
                String host = node.substring(0, splitIndex);
                int port = Integer.valueOf(node.substring(splitIndex + 1));
                hostAndPortsSet.add(new HostAndPort(host, port));
            }
        }
        return hostAndPortsSet;
    }
}
