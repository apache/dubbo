/**
 * Project: dubbo.registry-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2010-5-26
 * <p>
 * Copyright 1999-2010 Alibaba.com Croporation Limited.
 * All rights reserved.
 * <p>
 * This software is the confidential and proprietary information of
 * Alibaba Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Alibaba.com.
 */
package com.alibaba.dubbo.governance;

import com.alibaba.dubbo.registry.common.domain.User;

import java.util.Map;

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
