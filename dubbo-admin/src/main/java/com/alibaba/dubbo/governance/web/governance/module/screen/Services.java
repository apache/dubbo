/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.governance.module.screen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.OverrideService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.domain.Override;
import com.alibaba.dubbo.registry.common.util.Tool;

/**
 * Providers. URI: /services/$service/providers /addresses/$address/services /application/$application/services
 * 
 * @author ding.lid
 */
public class Services extends Restful {

    @Autowired
    private ProviderService providerService;
    
    @Autowired
    private ConsumerService consumerService;
    
    @Autowired
    private OverrideService overrideService;
    
    public void index(Map<String, Object> context) {
        String application = (String) context.get("application");
        String address = (String) context.get("address");
        
        if (context.get("service") == null
                && context.get("application") == null
                && context.get("address") == null) {
            context.put("service", "*");
        }
        
        List<String> providerServices = null;
        List<String> consumerServices = null;
        List<Override> overrides = null;
        if (application != null && application.length() > 0) {
            providerServices = providerService.findServicesByApplication(application);
            consumerServices = consumerService.findServicesByApplication(application);
            overrides = overrideService.findByApplication(application);
        } else if (address != null && address.length() > 0) {
            providerServices = providerService.findServicesByAddress(address);
            consumerServices = consumerService.findServicesByAddress(address);
            overrides = overrideService.findByAddress(Tool.getIP(address));
        } else {
            providerServices = providerService.findServices();
            consumerServices = consumerService.findServices();
            overrides = overrideService.findAll();
        }
        
        Set<String> services = new TreeSet<String>();
        if (providerServices != null) {
            services.addAll(providerServices);
        }
        if (consumerServices != null) {
            services.addAll(consumerServices);
        }
        
        context.put("providerServices", providerServices);
        context.put("consumerServices", consumerServices);
        context.put("services", services);
        
        Map<String, Override> service2Overrides = new HashMap<String, Override>();
        if (overrides != null && overrides.size() > 0 
                && services != null && services.size() > 0) {
            for (String s : services) {
                if (overrides != null && overrides.size() > 0) {
                    for (Override override : overrides) {
                        if (s.equals(override.getService()) && "*".equals(override.getAddress()) 
                                && ("*".equals(override.getApplication()) || (application != null && application.equals(override.getApplication())))) {
                            service2Overrides.put(s, override);
                        }
                    }
                }
            }
        }
        context.put("overrides", service2Overrides);
        
        String keyword = (String) context.get("keyword");
        if (keyword != null && !keyword.isEmpty()) {
            if("*".equals(keyword)){
                return;
            }
            keyword = keyword.toLowerCase();
            
            Set<String> newList = new HashSet<String>();
            Set<String> newProviders = new HashSet<String>();
            Set<String> newConsumers = new HashSet<String>();
            
            for (String o : services) {
                if (o.toLowerCase().toLowerCase().indexOf(keyword) != -1) {
                    newList.add(o);
                }
            }
            for (String o : providerServices) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newProviders.add(o);
                }
            }
            for (String o : consumerServices) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newConsumers.add(o);
                }
            }
            context.put("services", newList);
            context.put("providerServices", newProviders);
            context.put("consumerServices", newConsumers);
        }
    }
    
    // FIXME add Mock Operation here!
}
