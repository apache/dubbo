/**
 * Project: dubbo.core.service.server-1.0.6-SNAPSHOT
 * 
 * File Created at 2010-1-19
 * $Id: PageManager.java 34681 2010-01-19 07:00:49Z william.liangf $
 * 
 * Copyright 2008 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary page of
 * Alibaba Company. ("Confidential Page").  You shall not
 * disclose such Confidential Page and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.container.page;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PageManager
 * 
 * @author william.liangf
 */
public class PageManager {
    
    private static final PageManager INSTANCE = new PageManager();
    
    public static PageManager getInstance() {
        return INSTANCE;
    }
    
    private PageManager(){}
    
    private final Map<String, PageFactory> pageFactorys = new ConcurrentHashMap<String, PageFactory>();
    
    public void addPageFactory(PageFactory pageFactory) {
        this.pageFactorys.put(pageFactory.getUri(), pageFactory);
    }

    public void addPageFactorys(Collection<PageFactory> pageFactorys) {
        for (PageFactory pageFactory : pageFactorys) {
            addPageFactory(pageFactory);
        }
    }

    public void removePageFactory(String uri) {
        this.pageFactorys.remove(uri);
    }

    public void clearPageFactorys() {
        this.pageFactorys.clear();
    }

    public Collection<PageFactory> getPageFactorys() {
        TreeSet<PageFactory> set = new TreeSet<PageFactory>(new Comparator<PageFactory>() {
            public int compare(PageFactory o1, PageFactory o2) {
                return o1.getUri().compareTo(o2.getUri());
            }
        });
        set.addAll(pageFactorys.values());
        return set;
    }
    
    public PageFactory getPageFactory(String uri) {
        return this.pageFactorys.get(uri);
    }

    public Page getPage(String uri, Map<String, String> params) {
        PageFactory pageFactory = pageFactorys.get(uri);
        if (pageFactory == null)
            return null;
        return pageFactory.getPage(params);
    }


}
