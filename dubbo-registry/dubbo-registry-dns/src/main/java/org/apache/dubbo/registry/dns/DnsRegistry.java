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
import org.apache.dubbo.common.url.component.DubboServiceAddressURL;
import org.apache.dubbo.common.url.component.ServiceConfigURL;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.config.Constants;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.dns.util.DNSResolver;
import org.apache.dubbo.registry.support.CacheableFailbackRegistry;

import java.util.ArrayList;
import java.util.List;

public class DnsRegistry extends CacheableFailbackRegistry {

    private final DNSResolver dnsResolver;

    public DnsRegistry(URL url) {
        super(url);
        if ("DEFAULT_DNS_HOST".equals(url.getHost())) {
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
        int targetPost = url.getParameter(Constants.TARGET_PORT, 50051);
        String targetProtocol = url.getParameter(Constants.TARGET_PROTOCOL, "tri");

        dnsResolver.subscribe(dnsName, inetAddresses -> {
            List<URL> targetUrls = new ArrayList<>();
            for (java.net.InetAddress inetAddress : inetAddresses) {
                targetUrls.add(buildURL(url, targetProtocol, inetAddress.getHostAddress(), targetPost));
            }
            listener.notify(targetUrls);
        });
    }

    private URL buildURL(URL consumerURL, String protocol, String host, int port) {
        URL url = new ServiceConfigURL(protocol, host, port, consumerURL.getPath());
        return new DubboServiceAddressURL(url.getUrlAddress(), url.getUrlParam(), consumerURL, null);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {}

    @Override
    public void destroy() {
        dnsResolver.stop();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
