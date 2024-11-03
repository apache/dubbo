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
package org.apache.dubbo.registry.dns;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.url.component.DubboServiceAddressURL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.dns.util.DNSResolver;
import org.apache.dubbo.registry.support.CacheableFailbackRegistry;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class DnsRegistry extends CacheableFailbackRegistry {
    private final Logger logger = LoggerFactory.getLogger(DnsRegistry.class);

    private final DNSResolver dnsResolver;

    private final Map<SubscribeKey, DnsResultListener> subscribeListeners = new ConcurrentHashMap<>();

    public DnsRegistry(URL url) {
        super(url);
        if (Constants.DNS_DEFAULT_NAMESERVER.equals(url.getHost())) {
            this.dnsResolver = new DNSResolver();
        } else {
            this.dnsResolver = new DNSResolver(url.getHost(), url.getPort());
        }
    }

    @Override
    protected boolean isMatch(URL subscribeUrl, URL providerUrl) {
        return UrlUtils.isMatch(subscribeUrl, providerUrl);
    }

    @Override
    public void doRegister(URL url) {
        // no-op
    }

    @Override
    public void doUnregister(URL url) {
        // no-op
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        String dnsName = url.getParameter(Constants.DNS_NAME);
        if (dnsName == null) {
            throw new IllegalArgumentException("The value of 'dnsName' in the URL cannot be null.");
        }
        int targetPost = url.getParameter(Constants.TARGET_PORT, 50051);
        String targetProtocol = url.getParameter(Constants.TARGET_PROTOCOL, CommonConstants.TRIPLE);

        DnsResultListener dnsResultListener =
                subscribeListeners.computeIfAbsent(new SubscribeKey(url, listener), key -> inetAddresses -> {
                    logger.info("Resolved DNS name: " + dnsName + " to " + inetAddresses
                            + ". Start to notify ServiceKey: " + url.getServiceKey());
                    List<URL> targetUrls = new ArrayList<>();
                    for (InetAddress inetAddress : inetAddresses) {
                        targetUrls.add(buildURL(url, targetProtocol, inetAddress.getHostAddress(), targetPost));
                    }
                    listener.notify(targetUrls);
                    logger.info("Notified ServiceKey: " + url.getServiceKey() + " with " + targetUrls.size()
                            + " target URLs.");
                });

        dnsResolver.subscribe(dnsName, dnsResultListener);
        logger.info("Subscribed DNS name: " + dnsName + " with ServiceKey: " + url.getServiceKey());
    }

    private URL buildURL(URL consumerURL, String protocol, String host, int port) {
        URL url = new ServiceConfigURL(protocol, host, port, consumerURL.getPath());
        return new DubboServiceAddressURL(url.getUrlAddress(), url.getUrlParam(), consumerURL, null);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        String dnsName = url.getParameter(Constants.DNS_NAME);
        DnsResultListener dnsResultListener = subscribeListeners.remove(new SubscribeKey(url, listener));
        if (dnsResultListener != null) {
            dnsResolver.unsubscribe(dnsName, dnsResultListener);
            logger.info("Unsubscribed DNS name: " + dnsName + " with ServiceKey: " + url.getServiceKey());
        }
    }

    @Override
    public void destroy() {
        dnsResolver.stop();
        logger.info("DNS resolver stopped.");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private static class SubscribeKey {
        private final URL url;
        private final NotifyListener listener;

        public SubscribeKey(URL url, NotifyListener listener) {
            this.url = url;
            this.listener = listener;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SubscribeKey that = (SubscribeKey) o;
            return Objects.equals(url, that.url) && Objects.equals(listener, that.listener);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url, listener);
        }
    }
}
