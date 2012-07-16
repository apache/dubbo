/**
 * Project: dubbo.registry.server-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-6-28
 * 
 * Copyright 1999-2010 Alibaba.com Croporation Limited.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance.service.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.governance.sync.RegistryServerSync;
import com.alibaba.dubbo.registry.RegistryService;

/**
 * IbatisDAO
 * 
 * @author william.liangf
 */
public class AbstractService {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractService.class);

    @Autowired
    private RegistryServerSync           sync;
    
    @Autowired
    protected RegistryService registryService;
    
    public ConcurrentMap<String, ConcurrentMap<String, Map<Long, URL>>> getRegistryCache(){
        return sync.getRegistryCache();
    }
    
    
}
