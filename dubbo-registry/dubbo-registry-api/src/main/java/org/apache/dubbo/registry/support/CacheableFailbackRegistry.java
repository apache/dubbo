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
import org.apache.dubbo.common.url.component.ServiceAddressURL;
import org.apache.dubbo.common.url.component.URLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.UrlUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.dubbo.common.URLStrParser.ENCODED_QUESTION_MARK;
import static org.apache.dubbo.common.constants.CommonConstants.CHECK_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_SEPARATOR_ENCODED;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TAG_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.EMPTY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.OVERRIDE_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTE_PROTOCOL;

/**
 * Useful for registries who's sdk returns raw string as provider instance, for example, zookeeper and etcd.
 */
public abstract class CacheableFailbackRegistry extends FailbackRegistry {
    private final Map<String, String> extraParameters;

    protected final Map<URL, Map<String, ServiceAddressURL>> stringUrls = new HashMap<>();
    protected final Map<String, URLAddress> stringAddress = new HashMap<>();
    protected final Map<String, URLParam> stringParam = new HashMap<>();

    public CacheableFailbackRegistry(URL url) {
        super(url);
        extraParameters = new HashMap<>(8);
        extraParameters.put(CHECK_KEY, String.valueOf(false));
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
        URL copyOfConsumer = removeParamsFromConsumer(consumer);
        Map<String, ServiceAddressURL> consumerStringUrls = stringUrls.computeIfAbsent(consumer, (k) -> new ConcurrentHashMap<>());
        long firstUpdatedStamp = 0;
        for (String rawProvider : providers) {
            ServiceAddressURL cachedURL = consumerStringUrls.get(rawProvider);
            if (cachedURL == null) {
                cachedURL = createURL(rawProvider, copyOfConsumer, getExtraParameters());
                if (cachedURL == null) {
                    continue;
                }
                consumerStringUrls.put(rawProvider, cachedURL);
            } else {
                cachedURL.setCreatedStamp(System.currentTimeMillis());
            }
            if (firstUpdatedStamp == 0) {
                firstUpdatedStamp = cachedURL.getCreatedStamp();
            }
        }

        List<URL> list = new ArrayList<>(consumerStringUrls.size());
        Iterator<Map.Entry<String, ServiceAddressURL>> iterator = consumerStringUrls.entrySet().iterator();
        while (iterator.hasNext()) {
            ServiceAddressURL url = iterator.next().getValue();
            if (url.getCreatedStamp() - firstUpdatedStamp < 0) {
                iterator.remove();
            } else {
                list.add(url);
            }
        }

        return list;
    }

    private List<URL> toConfiguratorsWithoutEmpty(URL consumer, Collection<String> configurators) {
        List<URL> urls = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(configurators)) {
            for (String provider : configurators) {
                if (provider.contains(PROTOCOL_SEPARATOR_ENCODED)) {
                    URL url = URLStrParser.parseEncodedStr(provider);
                    if (UrlUtils.isMatch(consumer, url)) {
                        urls.add(url);
                    }
                }
            }
        }
        return urls;
    }

    protected List<URL> toUrlsWithEmpty(URL consumer, String path, Collection<String> providers) {
        List<URL> urls;
        if (CollectionUtils.isEmpty(providers)) {
            urls = new ArrayList<>(1);
        } else {
            String rawProvider = providers.iterator().next();
            if (rawProvider.startsWith(OVERRIDE_PROTOCOL) || rawProvider.startsWith(ROUTE_PROTOCOL)) {
                urls = toConfiguratorsWithoutEmpty(consumer, providers);
            } else {
                urls = toUrlsWithoutEmpty(consumer, providers);
            }
        }
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

    protected ServiceAddressURL createURL(String rawProvider, URL consumerURL, Map<String, String> extraParameters) {
        boolean encoded = true;
        // use encoded value directly to avoid URLDecoder.decode allocation.
        int paramStartIdx = rawProvider.indexOf(ENCODED_QUESTION_MARK);
        if (paramStartIdx == -1) {// if ENCODED_QUESTION_MARK does not shown, mark as not encoded.
            encoded = false;
        }
        String[] parts = URLStrParser.parseRawURLToArrays(rawProvider, paramStartIdx);
        if (parts.length <= 1) {
            logger.warn("Received url without any parameters " + rawProvider);
            return ServiceAddressURL.valueOf(rawProvider, consumerURL);
        }

        String rawAddress = parts[0];
        String rawParams = parts[1];
        URLAddress address = stringAddress.get(rawAddress);
        if (address == null) {
            address = URLAddress.parse(rawAddress, getDefaultURLProtocol(), encoded);
            stringAddress.put(rawAddress, address);
        } else {
            address.setTimestamp(System.currentTimeMillis());
        }

        URLParam param = stringParam.get(rawParams);
        if (param == null) {
            param = URLParam.parse(rawParams, encoded, extraParameters);
            stringParam.put(rawParams, param);
        } else {
            param.setTimestamp(System.currentTimeMillis());
        }

        ServiceAddressURL cachedURL = ServiceAddressURL.valueOf(address, param, consumerURL);
        if (isMatch(consumerURL, cachedURL)) {
            return cachedURL;
        }
        return null;
    }

    protected URL removeParamsFromConsumer(URL consumer) {
        return consumer.removeParameters(RELEASE_KEY, DUBBO_VERSION_KEY, METHODS_KEY, TIMESTAMP_KEY, TAG_KEY);
    }

    protected Map<String, String> getExtraParameters() {
        return extraParameters;
    }

    protected String getDefaultURLProtocol() {
        return DUBBO;
    }

    protected abstract boolean isMatch(URL subscribeUrl, URL providerUrl);

}
