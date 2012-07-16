/**
 * Project: dubbo.registry.console-2.2.0-SNAPSHOT
 * 
 * File Created at Mar 21, 2012
 * $Id: RegistryServerSync.java 182143 2012-06-27 03:25:50Z tony.chenl $
 * 
 * Copyright 1999-2100 Alibaba.com Corporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.sync;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.RegistryService;

/**
 * @author ding.lid
 */
public class RegistryServerSync implements InitializingBean, DisposableBean, NotifyListener {

    private static final Logger logger = LoggerFactory.getLogger(RegistryServerSync.class);

    private static final URL SUBSCRIBE = new URL(Constants.ADMIN_PROTOCOL, NetUtils.getLocalHost(), 0, "",
                                            Constants.INTERFACE_KEY, Constants.ANY_VALUE, 
                                            Constants.GROUP_KEY, Constants.ANY_VALUE, 
                                            Constants.VERSION_KEY, Constants.ANY_VALUE,
                                            Constants.CLASSIFIER_KEY, Constants.ANY_VALUE,
                                            Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY + "," 
                                                    + Constants.CONSUMERS_CATEGORY + ","
                                                    + Constants.ROUTERS_CATEGORY + ","
                                                    + Constants.CONFIGURATORS_CATEGORY,
                                            Constants.ENABLED_KEY, Constants.ANY_VALUE,
                                            Constants.CHECK_KEY, String.valueOf(false));

    private static final AtomicLong ID = new AtomicLong();

    @Autowired
    private RegistryService registryService;

    // ConcurrentMap<category, ConcurrentMap<servicename, Map<Long, URL>>>
    private final ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> registryCache = new ConcurrentHashMap<String, ConcurrentMap<String, Map<Long, URL>>>();

    public ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> getRegistryCache(){
        return registryCache;
    }
    
    public void afterPropertiesSet() throws Exception {
        logger.info("Init Dubbo Admin Sync Cache...");
        registryService.subscribe(SUBSCRIBE, this);
    }

    public void destroy() throws Exception {
        registryService.unsubscribe(SUBSCRIBE, this);
    }
    
    // 收到的通知对于 ，同一种类型数据（override、subcribe、route、其它是Provider），同一个服务的数据是全量的
    public void notify(List<URL> urls) {
        if(urls == null || urls.isEmpty()) {
        	return;
        }
        // Map<category, Map<servicename, Map<Long, URL>>>
        final Map<String, Map<String, Map<Long, URL>>> categories = new HashMap<String, Map<String, Map<Long, URL>>>();
        for(URL url : urls) {
        	// 分类
            String category = url.getParameter(Constants.CATEGORY_KEY, Constants.PROVIDERS_CATEGORY);
            Map<String, Map<Long, URL>> services = categories.get(category);
            if(services == null) {
                services = new HashMap<String, Map<Long,URL>>();
                categories.put(category, services);
            }
            // 接口
            String service = url.getServiceInterface();
            Map<Long, URL> ids = services.get(service);
            if(ids == null) {
                ids = new HashMap<Long, URL>();
                services.put(service, ids);
            }
            // 标识
            if(Constants.EMPTY_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
                ids.clear();
            } else {
                ids.put(ID.incrementAndGet(), url);
            }
        }
        for(Map.Entry<String, Map<String, Map<Long, URL>>> categoryEntry : categories.entrySet()) {
            String category = categoryEntry.getKey();
            for(Map.Entry<String, Map<Long, URL>> serviceEntry : categoryEntry.getValue().entrySet()) {
                String service = serviceEntry.getKey();
                Map<Long, URL> ids = serviceEntry.getValue();
                ConcurrentMap<String, Map<Long, URL>> services = registryCache.get(category);
                if(ids == null || ids.isEmpty()) {
                    if(services != null) {
                    	services.remove(service);
                    }
                } else {
                    if(services == null) {
                        services = new ConcurrentHashMap<String, Map<Long,URL>>();
                        registryCache.put(category, services);
                    }
                    services.put(service, ids);
                }
            }
        }
    }
}
    
