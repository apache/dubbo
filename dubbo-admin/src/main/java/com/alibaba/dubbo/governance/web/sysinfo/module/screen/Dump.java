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
package com.alibaba.dubbo.governance.web.sysinfo.module.screen;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Consumer;
import com.alibaba.dubbo.registry.common.domain.Provider;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class Dump extends Restful {

    @Autowired
    ProviderService providerDAO;

    @Autowired
    ConsumerService consumerDAO;

    @Autowired
    HttpServletResponse response;

    public void noProviders(Map<String, Object> context) throws IOException {
        PrintWriter writer = response.getWriter();
        List<String> sortedService = getNoProviders();
        Collections.sort(sortedService);
        writer.println(sortedService.size() + " services don't have provider");
        for (String noProvider : sortedService) {
            writer.println(noProvider);
        }
        writer.flush();
        response.setContentType("text/plain");
    }

    public void services(Map<String, Object> context) throws IOException {
        PrintWriter writer = response.getWriter();
        List<String> sortedService = providerDAO.findServices();
        Collections.sort(sortedService);
        writer.println(sortedService.size() + " services");
        for (String service : sortedService) {
            writer.println(service + (providerDAO.findByService(service).size()));
        }
        writer.flush();
        response.setContentType("text/plain");
    }

    public void providers(Map<String, Object> context) throws IOException {
        PrintWriter writer = response.getWriter();
        List<Provider> providers = providerDAO.findAll();
        List<String> sortedProviders = new ArrayList<String>();
        for (Provider provider : providers) {
            sortedProviders.add(provider.getUrl() + " " + provider.getService());
        }
        Collections.sort(sortedProviders);
        writer.println(sortedProviders.size() + " provider instance");
        for (String provider : sortedProviders) {
            writer.println(provider);
        }
        writer.flush();
        response.setContentType("text/plain");
    }

    public void consumers(Map<String, Object> context) throws IOException {
        PrintWriter writer = response.getWriter();
        List<Consumer> consumers = consumerDAO.findAll();
        List<String> sortedConsumerss = new ArrayList<String>();
        for (Consumer consumer : consumers) {
            sortedConsumerss.add(consumer.getAddress() + " " + consumer.getService());
        }
        Collections.sort(sortedConsumerss);
        writer.println(sortedConsumerss.size() + " consumer instance");
        for (String consumer : sortedConsumerss) {
            writer.println(consumer);
        }
        writer.flush();
        response.setContentType("text/plain");
    }

    public void versions(Map<String, Object> context) throws IOException {
        PrintWriter writer = response.getWriter();
        List<Provider> providers = providerDAO.findAll();
        List<Consumer> consumers = consumerDAO.findAll();
        Set<String> parametersSet = new HashSet<String>();
        Map<String, Set<String>> versions = new HashMap<String, Set<String>>();
        for (Provider provider : providers) {
            parametersSet.add(provider.getParameters());
        }
        for (Consumer consumer : consumers) {
            parametersSet.add(consumer.getParameters());
        }
        Iterator<String> temp = parametersSet.iterator();
        while (temp.hasNext()) {
            Map<String, String> parameter = StringUtils.parseQueryString(temp.next());
            if (parameter != null) {
                String dubboversion = parameter.get("dubbo");
                String app = parameter.get("application");
                if (versions.get(dubboversion) == null) {
                    Set<String> apps = new HashSet<String>();
                    versions.put(dubboversion, apps);
                }
                versions.get(dubboversion).add(app);
            }
        }
        for (String version : versions.keySet()) {
            writer.println("dubbo version: " + version);
            writer.println(StringUtils.join(versions.get(version), "\n"));
            writer.println("\n");
        }
        context.put("versions", versions);
        writer.flush();
        response.setContentType("text/plain");
    }

    private List<String> getNoProviders() {
        List<String> providerServices = providerDAO.findServices();
        List<String> consumerServices = consumerDAO.findServices();
        List<String> noProviderServices = new ArrayList<String>();
        if (consumerServices != null) {
            noProviderServices.addAll(consumerServices);
            noProviderServices.removeAll(providerServices);
        }
        return noProviderServices;
    }
}
