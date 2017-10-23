/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-4-15
 * $Id: ConsumerServiceImpl.java 184666 2012-07-05 11:13:17Z tony.chenl $
 * <p>
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.service.impl;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.governance.service.ConsumerService;
import com.alibaba.dubbo.governance.sync.util.Pair;
import com.alibaba.dubbo.governance.sync.util.SyncUtils;
import com.alibaba.dubbo.registry.common.domain.Consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author william.liangf
 */
public class ConsumerServiceImpl extends AbstractService implements ConsumerService {

    public List<Consumer> findByService(String service) {
        return SyncUtils.url2ConsumerList(findConsumerUrlByService(service));
    }

    public Consumer findConsumer(Long id) {
        return SyncUtils.url2Consumer(findConsumerUrl(id));
    }

    private Pair<Long, URL> findConsumerUrl(Long id) {
        return SyncUtils.filterFromCategory(getRegistryCache(), Constants.CONSUMERS_CATEGORY, id);
    }

    public List<Consumer> findAll() {
        return SyncUtils.url2ConsumerList(findAllConsumerUrl());
    }

    private Map<Long, URL> findAllConsumerUrl() {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<String> findAddresses() {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<Long, URL>> consumerUrls = getRegistryCache().get(Constants.CONSUMERS_CATEGORY);
        if (null == consumerUrls) return ret;

        for (Map.Entry<String, Map<Long, URL>> e1 : consumerUrls.entrySet()) {
            Map<Long, URL> value = e1.getValue();
            for (Map.Entry<Long, URL> e2 : value.entrySet()) {
                URL u = e2.getValue();
                String app = u.getAddress();
                if (app != null) ret.add(app);
            }
        }

        return ret;
    }

    public List<String> findAddressesByApplication(String application) {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<Long, URL>> consumerUrls = getRegistryCache().get(Constants.CONSUMERS_CATEGORY);

        if(consumerUrls == null)
            return ret;

        for (Map.Entry<String, Map<Long, URL>> e1 : consumerUrls.entrySet()) {
            Map<Long, URL> value = e1.getValue();
            for (Map.Entry<Long, URL> e2 : value.entrySet()) {
                URL u = e2.getValue();
                if (application.equals(u.getParameter(Constants.APPLICATION_KEY))) {
                    String addr = u.getAddress();
                    if (addr != null) ret.add(addr);
                }
            }
        }

        return ret;
    }

    public List<String> findAddressesByService(String service) {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<Long, URL>> consumerUrls = getRegistryCache().get(Constants.CONSUMERS_CATEGORY);
        if (null == consumerUrls) return ret;

        for (Map.Entry<Long, URL> e2 : consumerUrls.get(service).entrySet()) {
            URL u = e2.getValue();
            String app = u.getAddress();
            if (app != null) ret.add(app);
        }

        return ret;
    }

    public List<Consumer> findByAddress(String consumerAddress) {
        return SyncUtils.url2ConsumerList(findConsumerUrlByAddress(consumerAddress));
    }

    public List<String> findServicesByAddress(String address) {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<Long, URL>> consumerUrls = getRegistryCache().get(Constants.CONSUMERS_CATEGORY);
        if (consumerUrls == null || address == null || address.length() == 0) return ret;

        for (Map.Entry<String, Map<Long, URL>> e1 : consumerUrls.entrySet()) {
            Map<Long, URL> value = e1.getValue();
            for (Map.Entry<Long, URL> e2 : value.entrySet()) {
                URL u = e2.getValue();
                if (address.equals(u.getAddress())) {
                    ret.add(e1.getKey());
                    break;
                }
            }
        }

        return ret;
    }

    private Map<Long, URL> findConsumerUrlByAddress(String address) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
        filter.put(SyncUtils.ADDRESS_FILTER_KEY, address);

        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<String> findApplications() {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<Long, URL>> consumerUrls = getRegistryCache().get(Constants.CONSUMERS_CATEGORY);
        if (consumerUrls == null) return ret;

        for (Map.Entry<String, Map<Long, URL>> e1 : consumerUrls.entrySet()) {
            Map<Long, URL> value = e1.getValue();
            for (Map.Entry<Long, URL> e2 : value.entrySet()) {
                URL u = e2.getValue();
                String app = u.getParameter(Constants.APPLICATION_KEY);
                if (app != null) ret.add(app);
            }
        }

        return ret;
    }

    public List<String> findApplicationsByServiceName(String service) {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<Long, URL>> consumerUrls = getRegistryCache().get(Constants.CONSUMERS_CATEGORY);
        if (consumerUrls == null) return ret;

        Map<Long, URL> value = consumerUrls.get(service);
        if (value == null) {
            return ret;
        }
        for (Map.Entry<Long, URL> e2 : value.entrySet()) {
            URL u = e2.getValue();
            String app = u.getParameter(Constants.APPLICATION_KEY);
            if (app != null) ret.add(app);
        }

        return ret;
    }

    public List<Consumer> findByApplication(String application) {
        return SyncUtils.url2ConsumerList(findConsumerUrlByApplication(application));
    }

    private Map<Long, URL> findConsumerUrlByApplication(String application) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
        filter.put(Constants.APPLICATION_KEY, application);

        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

    public List<String> findServicesByApplication(String application) {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<Long, URL>> consumerUrls = getRegistryCache().get(Constants.CONSUMERS_CATEGORY);
        if (consumerUrls == null || application == null || application.length() == 0) return ret;

        for (Map.Entry<String, Map<Long, URL>> e1 : consumerUrls.entrySet()) {
            Map<Long, URL> value = e1.getValue();
            for (Map.Entry<Long, URL> e2 : value.entrySet()) {
                URL u = e2.getValue();
                if (application.equals(u.getParameter(Constants.APPLICATION_KEY))) {
                    ret.add(e1.getKey());
                    break;
                }
            }
        }

        return ret;
    }

    public List<String> findServices() {
        List<String> ret = new ArrayList<String>();
        ConcurrentMap<String, Map<Long, URL>> consumerUrls = getRegistryCache().get(Constants.CONSUMERS_CATEGORY);
        if (consumerUrls != null) ret.addAll(consumerUrls.keySet());
        return ret;
    }

    public Map<Long, URL> findConsumerUrlByService(String service) {
        Map<String, String> filter = new HashMap<String, String>();
        filter.put(Constants.CATEGORY_KEY, Constants.CONSUMERS_CATEGORY);
        filter.put(SyncUtils.SERVICE_FILTER_KEY, service);

        return SyncUtils.filterFromCategory(getRegistryCache(), filter);
    }

}
