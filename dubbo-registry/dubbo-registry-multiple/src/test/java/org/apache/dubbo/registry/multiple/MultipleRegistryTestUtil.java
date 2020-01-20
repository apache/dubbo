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
package org.apache.dubbo.registry.multiple;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.ListenerRegistryWrapper;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.redis.RedisRegistry;
import org.apache.dubbo.registry.zookeeper.ZookeeperRegistry;
import org.apache.dubbo.rpc.RpcException;

import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.dubbo.common.constants.RegistryConstants.APP_DYNAMIC_CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.COMPATIBLE_CONFIG_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.DYNAMIC_CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTE_PROTOCOL;

/**
 * 2019-05-13
 */
public class MultipleRegistryTestUtil {
    public static ZookeeperRegistry getZookeeperRegistry(Collection<Registry> registryCollection) {
        for (Registry registry : registryCollection) {
            if (registry instanceof ListenerRegistryWrapper) {
                registry = ((ListenerRegistryWrapper) registry).getRegistry();
            }
            if (registry instanceof ZookeeperRegistry) {
                return (ZookeeperRegistry) registry;
            }
        }
        return null;
    }

    public static RedisRegistry getRedisRegistry(Collection<Registry> registryCollection) {
        for (Registry registry : registryCollection) {
            if (registry instanceof ListenerRegistryWrapper) {
                registry = ((ListenerRegistryWrapper) registry).getRegistry();
            }
            if (registry instanceof RedisRegistry) {
                return (RedisRegistry) registry;
            }
        }
        return null;
    }

    public static String getRedisContent(int port, String key) {
        Jedis jedis = null;
        try {
            jedis = new Jedis("127.0.0.1", port);
            return jedis.get(key);
        } catch (Throwable e) {
            throw new RpcException("Failed to put to redis . cause: " + e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static String getRedisHashContent(int port, String key, String field) {
        Jedis jedis = null;
        try {
            jedis = new Jedis("127.0.0.1", port);
            return jedis.hget(key, field);
        } catch (Throwable e) {
            throw new RpcException("Failed to put to redis . cause: " + e.getMessage(), e);
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * copy from @org.apache.dubbo.registry.integration.RegistryDirectory#notify(java.util.List)
     *
     * @param urls
     * @return
     */
    public static List<URL> getProviderURLsFromNotifyURLS(List<URL> urls) {
        Map<String, List<URL>> categoryUrls = urls.stream()
                .filter(Objects::nonNull)
                .filter(MultipleRegistryTestUtil::isValidCategory)
                .filter(MultipleRegistryTestUtil::isNotCompatibleFor26x)
                .collect(Collectors.groupingBy(url -> {
                    if (UrlUtils.isConfigurator(url)) {
                        return CONFIGURATORS_CATEGORY;
                    } else if (UrlUtils.isRoute(url)) {
                        return ROUTERS_CATEGORY;
                    } else if (UrlUtils.isProvider(url)) {
                        return PROVIDERS_CATEGORY;
                    }
                    return "";
                }));

        // providers
        List<URL> providerURLs = categoryUrls.getOrDefault(PROVIDERS_CATEGORY, Collections.emptyList());
        return providerURLs;

    }

    private static boolean isValidCategory(URL url) {
        String category = url.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
        if ((ROUTERS_CATEGORY.equals(category) || ROUTE_PROTOCOL.equals(url.getProtocol())) ||
                PROVIDERS_CATEGORY.equals(category) ||
                CONFIGURATORS_CATEGORY.equals(category) || DYNAMIC_CONFIGURATORS_CATEGORY.equals(category) ||
                APP_DYNAMIC_CONFIGURATORS_CATEGORY.equals(category)) {
            return true;
        }
        return false;
    }

    private static boolean isNotCompatibleFor26x(URL url) {
        return StringUtils.isEmpty(url.getParameter(COMPATIBLE_CONFIG_KEY));
    }
}
