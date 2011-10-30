/**
 * Project: dubbo.core.service.server-1.0.6-SNAPSHOT
 * 
 * File Created at 2010-1-19
 * $Id: ServiceInformationProvider.java 35106 2010-01-22 10:59:42Z william.liangf $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.container.page.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.common.Extension;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.container.page.Page;
import com.alibaba.dubbo.container.page.PageHandler;
import com.alibaba.dubbo.registry.Registry;
import com.alibaba.dubbo.registry.support.AbstractRegistry;
import com.alibaba.dubbo.registry.support.AbstractRegistryFactory;

/**
 * ServicePageHandler
 * 
 * @author william.liangf
 */
@Extension("service")
public class ServicePageHandler implements PageHandler {

    public Page handle(URL url) {
        List<List<String>> rows = new ArrayList<List<String>>();
        Collection<Registry> registries = AbstractRegistryFactory.getRegistries();
        if (registries != null && registries.size() > 0) {
        	Registry registry = registries.iterator().next();
        	if (registry instanceof AbstractRegistry) {
            	Map<String, List<URL>> services = ((AbstractRegistry) registry).getRegistered();
                if (services != null && services.size() > 0) {
                    for (Map.Entry<String, List<URL>> entry : services.entrySet()) {
                    	String service = entry.getKey();
                    	List<URL> urls = entry.getValue();
                    	for (URL ue : urls) {
    	                    List<String> row = new ArrayList<String>();
    	                    row.add(service.replace("<", "&lt;").replace(">", "&gt;"));
    	                    row.add(ue.toString().replace("<", "&lt;").replace(">", "&gt;"));
    	                    rows.add(row);
                    	}
                    }
                }
        	}
        }
        return new Page("<a href=\"/\">Home</a> &gt; Service", "Services (" + rows.size() + ")",
                new String[] { "Service Type:", "URL:" }, rows);
    }

}
