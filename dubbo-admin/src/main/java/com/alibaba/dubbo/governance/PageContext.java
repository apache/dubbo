/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * 
 * File Created at 2010-5-26
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
package com.alibaba.dubbo.governance;

import java.util.Map;

import com.alibaba.dubbo.registry.common.domain.User;

/**
 * Context
 * 
 * @author william.liangf
 */
public interface PageContext {

    public String get(String key);
    
    public String[] gets(String key);

    public Map<String, String[]> getAll();

    public void put(String key, Object value);
    
    public String getMessage(String key, Object... args);

    public String getClientAddress();

    public String getOperateAddress();

    public String getRegistryAddress();
    
    public String getURI();
    
    public String getURL();
    
    public String getReferer();

    public User getLoginUser();

}
