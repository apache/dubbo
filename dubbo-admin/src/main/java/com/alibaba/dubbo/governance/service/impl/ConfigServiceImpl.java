/**
 * Project: dubbo.registry.server-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-6-30
 * $Id: ConfigServiceImpl.java 181735 2012-06-26 02:31:34Z tony.chenl $
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
package com.alibaba.dubbo.governance.service.impl;

import java.util.List;
import java.util.Map;

import com.alibaba.dubbo.governance.service.ConfigService;
import com.alibaba.dubbo.registry.common.domain.Config;

/**
 * TODO Comment of IbatisConfigDAO
 * @author rain.chenjr
 *
 */
public class ConfigServiceImpl extends AbstractService implements ConfigService{

    /* (non-Javadoc)
     * @see com.alibaba.dubbo.governance.service.ConfigService#update(java.util.List)
     */
    public void update(List<Config> configs) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see com.alibaba.dubbo.governance.service.ConfigService#findAllConfigsMap()
     */
    public Map<String, String> findAllConfigsMap() {
        // TODO Auto-generated method stub
        return null;
    }
}
