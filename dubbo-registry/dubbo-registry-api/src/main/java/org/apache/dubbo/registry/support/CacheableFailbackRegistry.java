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
package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.UrlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;

/**
 * Useful for registries who's sdk returns raw string as provider instance, for example, zookeeper and etcd.
 */
public abstract class CacheableFailbackRegistry extends FailbackRegistry {

    protected final ConcurrentMap<URL, ConcurrentMap<String, URL>> stringUrls = new ConcurrentHashMap<>();

    public CacheableFailbackRegistry(URL url) {
        super(url);
    }

    protected List<URL> toUrlsWithoutEmpty(URL consumer, List<String> providers) {
        if (CollectionUtils.isNotEmpty(providers)) {
            Map<String, URL> consumerStringUrls = stringUrls.computeIfAbsent(consumer, (k) -> new ConcurrentHashMap<>());
            Map<String, URL> copyOfStringUrls = new HashMap<>(consumerStringUrls);
            for (String rawProvider : providers) {
                URL cachedUrl = copyOfStringUrls.remove(rawProvider);
                if (cachedUrl == null) {
                    // parse encoded (URLEncoder.encode) url directly.
                    URL url = URL.valueOf(rawProvider, true);
                    if (isMatch(consumer, url)) {
                        consumerStringUrls.put(rawProvider, url);
                    }
                }
            }
            copyOfStringUrls.keySet().forEach(consumerStringUrls::remove);

            List<URL> urls = new ArrayList<>(consumerStringUrls.size());
            consumerStringUrls.values().forEach(u -> urls.add(UrlUtils.newModifiableUrl(u)));
            return urls;
        }

        stringUrls.remove(consumer);
        return new ArrayList<>(1);
    }

    protected List<URL> toUrlsWithEmpty(URL consumer, String path, List<String> providers) {
        List<URL> urls = toUrlsWithoutEmpty(consumer, providers);
        if (urls.isEmpty()) {
            int i = path.lastIndexOf(PATH_SEPARATOR);
            String category = i < 0 ? path : path.substring(i + 1);
            URL empty = URLBuilder.from(consumer)
                    .setProtocol(EMPTY_PROTOCOL)
                    .addParameter(CATEGORY_KEY, category)
                    .build();
            urls.add(empty);
        }
        return urls;
    }

    protected abstract boolean isMatch(URL subscribeUrl, URL providerUrl);

}
