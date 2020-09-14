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
import org.apache.dubbo.common.URLStrParser;
import org.apache.dubbo.common.url.component.InterfaceAddressURL;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;

/**
 * Useful for registries who's sdk returns raw string as provider instance, for example, zookeeper and etcd.
 */
public abstract class CacheableFailbackRegistry extends FailbackRegistry {
    protected final Map<URL, Map<String, InterfaceAddressURL>> stringUrls = new HashMap<>();
    protected final Map<String, URLAddress> stringAddress = new HashMap<>();
    protected final Map<String, URLParam> stringParam = new HashMap<>();

    public CacheableFailbackRegistry(URL url) {
        super(url);
    }

    /**
     * TODO
     * 1. tackle path and interface keys to further improve cache utilization between interfaces.
     * 2. enable simplified mode on Provider side to remove timestamp key from provider URL.
     *
     * @param consumer
     * @param providers
     * @return
     */
    protected List<URL> toUrlsWithoutEmpty(URL consumer, Collection<String> providers) {
        if (CollectionUtils.isNotEmpty(providers)) {
            URL copyOfConsumer = removeParamsFromConsumer(consumer);
            Map<String, InterfaceAddressURL> consumerStringUrls = stringUrls.computeIfAbsent(consumer, (k) -> new ConcurrentHashMap<>());
            long firstUpdatedStamp = 0l;
            for (String rawProvider : providers) {
                InterfaceAddressURL cachedURL = consumerStringUrls.get(rawProvider);
                if (cachedURL == null) {
                    // use encoded value directly to avoid URLDecoder.decode allocation.
                    String[] parts = URLStrParser.parseEncodedStrToArrays(rawProvider);
                    if (parts.length <= 1) {
                        logger.warn("Received url without any parameters " + rawProvider);
                        consumerStringUrls.put(rawProvider, InterfaceAddressURL.valueOf(rawProvider, copyOfConsumer));
                        break;
                    }

                    String rawAddress = parts[0];
                    String rawParams = parts[1];
                    URLAddress address = stringAddress.get(rawAddress);
                    if (address == null) {
                        address = URLAddress.parseEncoded(rawAddress, DUBBO);
                    }
                    URLParam param = stringParam.get(rawParams);
                    if (param == null) {
                        param = URLParam.parseEncoded(rawParams);
                    }

                    cachedURL = InterfaceAddressURL.valueOf(address, param, copyOfConsumer);
                    if (isMatch(consumer, cachedURL)) {
                        consumerStringUrls.put(rawProvider, cachedURL);
                    }
                } else {
                    cachedURL.setCreatedStamp(System.currentTimeMillis());
                }
                if (firstUpdatedStamp == 0) {
                    firstUpdatedStamp = cachedURL.getCreatedStamp();
                }
            }

            List<URL> list = new ArrayList<>(consumerStringUrls.size());
            Iterator<Map.Entry<String, InterfaceAddressURL>> iterator = consumerStringUrls.entrySet().iterator();
            while (iterator.hasNext()) {
                InterfaceAddressURL url = iterator.next().getValue();
                if (url.getCreatedStamp() - firstUpdatedStamp < 0) {
                    iterator.remove();
                } else {
                    list.add(url);
                }
            }

            return list;
        }

        return new ArrayList<>(1);
    }

    protected List<URL> toUrlsWithEmpty(URL consumer, String path, Collection<String> providers) {
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

    private URL removeParamsFromConsumer(URL consumer) {
        return consumer.removeParameters(RELEASE_KEY, DUBBO_VERSION_KEY, METHODS_KEY, TIMESTAMP_KEY, TAG_KEY);
    }

}
