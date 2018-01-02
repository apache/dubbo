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
package com.alibaba.dubbo.governance.web.governance.module.screen;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Providers.
 * URI: /services/$service/providers
 *
 */
public class Addresses extends Restful {

    @Autowired
    private ProviderService providerService;

    @Autowired
    private ConsumerService consumerService;

    public void index(Map<String, Object> context) {
        String application = (String) context.get("application");
        String service = (String) context.get("service");
        List<String> providerAddresses = null;
        List<String> consumerAddresses = null;

        if (application != null && application.length() > 0) {
            providerAddresses = providerService.findAddressesByApplication(application);
            consumerAddresses = consumerService.findAddressesByApplication(application);
        } else if (service != null && service.length() > 0) {
            providerAddresses = providerService.findAddressesByService(service);
            consumerAddresses = consumerService.findAddressesByService(service);
        } else {
            providerAddresses = providerService.findAddresses();
            consumerAddresses = consumerService.findAddresses();
        }

        Set<String> addresses = new TreeSet<String>();
        if (providerAddresses != null) {
            addresses.addAll(providerAddresses);
        }
        if (consumerAddresses != null) {
            addresses.addAll(consumerAddresses);
        }
        context.put("providerAddresses", providerAddresses);
        context.put("consumerAddresses", consumerAddresses);
        context.put("addresses", addresses);

        if (context.get("service") == null
                && context.get("application") == null
                && context.get("address") == null) {
            context.put("address", "*");
        }

        String keyword = (String) context.get("keyword");
        if (StringUtils.isNotEmpty(keyword)) {
            if ("*".equals(keyword)) return;

            keyword = keyword.toLowerCase();
            Set<String> newList = new HashSet<String>();
            Set<String> newProviders = new HashSet<String>();
            Set<String> newConsumers = new HashSet<String>();

            for (String o : addresses) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newList.add(o);
                }
            }
            for (String o : providerAddresses) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newProviders.add(o);
                }
            }
            for (String o : consumerAddresses) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newConsumers.add(o);
                }
            }
            context.put("addresses", newList);
            context.put("providerAddresses", newProviders);
            context.put("consumerAddresses", newConsumers);
        }
    }

    public void search(Map<String, Object> context) {
        index(context);

        Set<String> newList = new HashSet<String>();
        @SuppressWarnings("unchecked")
        Set<String> list = (Set<String>) context.get("addresses");
        String keyword = (String) context.get("keyword");
        if (StringUtils.isNotEmpty(keyword)) {
            keyword = keyword.toLowerCase();
            for (String o : list) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newList.add(o);
                }
            }
        }
        context.put("addresses", newList);
    }
}
