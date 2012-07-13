/**
 * Project: dubbo.registry.console-2.1.0-SNAPSHOT
 * 
 * File Created at Oct 31, 2011
 * $Id: ContextUtil.java 181192 2012-06-21 05:05:47Z tony.chenl $
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
package com.alibaba.dubbo.governance.web.util;

import java.util.Map;

/**
 * TODO Comment of ContextUtil
 * @author haomin.liuhm
 *
 */
public class ContextUtil {
    
    private ContextUtil(Map<String, Object> c){
    }
    
    public static Object get(Map<String, Object> context, Object key, Object defaultv){
        Object res = context.get(key);
        if(res == null){
            res = defaultv;
        }
        return res;
    }
}

