/*
 * Copyright 2011 Alibaba.com All right reserved. This software is the
 * confidential and proprietary information of Alibaba.com ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with Alibaba.com.
 */
package com.alibaba.dubbo.governance.web.governance.module.screen;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.service.ProviderService;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;

/**
 * Providers.
 * URI: /applications
 * 
 * @author william.liangf
 */
public class Applications extends Restful {
    
    @Autowired
    private ProviderService providerService;
    
    @Autowired
    private ConsumerService consumerService;

    public void index(Map<String, Object> context) {
        if (context.get("service") == null
                && context.get("application") == null
                && context.get("address") == null) {
            context.put("application", "*");
        }
        String keyword = (String) context.get("keyword");
        if (StringUtils.isNotEmpty(keyword)) {
            Set<String> applications = new TreeSet<String>();
            List<String> providerApplications = providerService.findApplications();
            if (providerApplications != null && providerApplications.size() > 0) {
                applications.addAll(providerApplications);
            }
            List<String> consumerApplications = consumerService.findApplications();
            if (consumerApplications != null && consumerApplications.size() > 0) {
                applications.addAll(consumerApplications);
            }
            keyword = keyword.toLowerCase();
            Set<String> newList = new HashSet<String>();
            Set<String> newProviders = new HashSet<String>();
            Set<String> newConsumers = new HashSet<String>();
            if("*".equals(keyword)){
                context.put("applications", applications);
                context.put("providerApplications", providerApplications);
                context.put("consumerApplications", consumerApplications);
                return;
            }
            for (String o : applications) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newList.add(o);
                }
            }
            for (String o : providerApplications) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newProviders.add(o);
                }
            }
            for (String o : consumerApplications) {
                if (o.toLowerCase().indexOf(keyword) != -1) {
                    newConsumers.add(o);
                }
            }
            context.put("applications", newList);
            context.put("providerApplications", newProviders);
            context.put("consumerApplications", newConsumers);
        }
    }
    
    public void search(Map<String, Object> context) {
        index(context);
        
        Set<String> newList = new HashSet<String>();
        @SuppressWarnings("unchecked")
        Set<String> apps = (Set<String>)context.get("applications");
        String keyword = (String) context.get("keyword");
        if(StringUtils.isNotEmpty(keyword)){
            keyword = keyword.toLowerCase();
            for(String o : apps){
                if(o.toLowerCase().indexOf(keyword)!=-1){
                    newList.add(o);
                }
            }
        }
        context.put("applications", newList);
    }

}
