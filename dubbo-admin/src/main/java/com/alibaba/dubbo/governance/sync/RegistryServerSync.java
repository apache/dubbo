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
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;

/**
 * @author ding.lid
 */
public class RegistryServerSync implements InitializingBean, DisposableBean {
    
    private static final Logger logger = LoggerFactory.getLogger(RegistryServerSync.class);
    
    static final URL SUBSCRIBE = new URL(Constants.ADMIN_PROTOCOL, NetUtils.getLocalHost(), 0, "",
                                            Constants.INTERFACE_KEY, Constants.ANY_VALUE, 
                                            Constants.GROUP_KEY, Constants.ANY_VALUE, 
                                            Constants.VERSION_KEY, Constants.ANY_VALUE,
                                            Constants.CLASSIFIER_KEY, Constants.ANY_VALUE,
                                            Constants.CATEGORY_KEY, Constants.ANY_VALUE,
                                            Constants.ENABLED_KEY, Constants.ANY_VALUE,
                                            Constants.CHECK_KEY, String.valueOf(false));

    static final AtomicLong ID = new AtomicLong(1);
    
    private String consoleUrl;
    
    @Autowired
    private Registry registry;
    
    public static class Pair<K, V> {
        public K key;
        public V value;
        
        public Pair(K k, V v) {
            key = k;
            value = v;
        }
    }
    
    public boolean isConnected() {
        return registry.isAvailable();
    }
    
    // ConcurrentMap<category, ConcurrentMap<servicename, Map<Long, URL>>>
    private final ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> category2Service2Urls = new ConcurrentHashMap<String, ConcurrentMap<String, Map<Long, URL>>>();

    static long generateId() {
        return ID.getAndIncrement();
    }
    
    public ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> getcategory2Service2Urls(){
        return category2Service2Urls;
    }
    
    NotifyListener notifyListener = new NotifyListener() {
        public void notify(List<URL> urls) {
            RegistryServerSync.this.notify(urls);
        }
    };
    
    public void setConsolePort(int consolePort) {
        this.consoleUrl = NetUtils.getLocalHost() + ":" + consolePort;
    }

    public String getConsoleUrl() {
        return consoleUrl;
    }

    public void afterPropertiesSet() throws Exception {
        logger.info("Init DubboGovernanceCache...");
        registry.subscribe(SUBSCRIBE, notifyListener);
    }

    public void destroy() throws Exception {
        registry.unsubscribe(SUBSCRIBE, notifyListener);
    }
    
    // 收到的通知对于 ，同一种类型数据（override、subcribe、route、其它是Provider），同一个服务的数据是全量的
    void notify(List<URL> notifyedUrls) {
        if(notifyedUrls == null || notifyedUrls.isEmpty()) return;
        
        // Map<category, Map<servicename, Map<Long, URL>>>
        final Map<String, Map<String, Map<Long, URL>>> categories = new HashMap<String, Map<String, Map<Long, URL>>>();
        for(URL url : notifyedUrls) {
            String c = url.getParameter(Constants.CATEGORY_KEY);
            if(StringUtils.isBlank(c)) c = Constants.PROVIDERS_CATEGORY;
            
            String s = url.getServiceKey();;
            
            Map<String, Map<Long, URL>> services = categories.get(c);
            if(services == null) {
                services = new HashMap<String, Map<Long,URL>>();
                categories.put(c, services);
            }

            Map<Long, URL> map = services.get(s);
            if(map == null) {
                map = new HashMap<Long, URL>();
                services.put(s, map);
            }
            
            if(Constants.EMPTY_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
                map.clear();
            }
            else {
                map.put(generateId(), url);
            }
        }
        
        
        for(Map.Entry<String, Map<String, Map<Long, URL>>> e : categories.entrySet()) {
            String c = e.getKey();
            
            for(Map.Entry<String, Map<Long, URL>> e2 : e.getValue().entrySet()) {
                String s = e2.getKey();
                Map<Long, URL> urls = e2.getValue();
                
                ConcurrentMap<String, Map<Long, URL>> services = category2Service2Urls.get(c);
                if(urls == null || urls.isEmpty()) {
                    if(services != null) services.remove(s);
                }
                else {
                    if(services == null) {
                        services = new ConcurrentHashMap<String, Map<Long,URL>>();
                        category2Service2Urls.put(c, services);
                    }
                    services.put(s, urls);
                }
            }
        }
    }
}
    
