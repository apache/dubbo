/**
 * Project: dubbo.registry.console-2.1.0-SNAPSHOT
 * 
 * File Created at Sep 13, 2011
 * $Id: Status.java 181192 2012-06-21 05:05:47Z tony.chenl $
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
package com.alibaba.dubbo.governance.web.sysinfo.module.screen;

import java.util.LinkedHashMap;
import java.util.Map;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.StatusManager;

/**
 * @author ding.lid
 */
public class Statuses extends Restful {
    public void index(Map<String, Object> context) throws Exception {
    	ExtensionLoader<StatusChecker> loader= ExtensionLoader.getExtensionLoader(StatusChecker.class);
    	Map<String, com.alibaba.dubbo.common.status.Status> statusList = new LinkedHashMap<String, com.alibaba.dubbo.common.status.Status>();
        for (String name : loader.getSupportedExtensions()) {
        	com.alibaba.dubbo.common.status.Status status = loader.getExtension(name).check();
        	if (status.getLevel() != null && status.getLevel() != com.alibaba.dubbo.common.status.Status.Level.UNKNOWN) {
        		statusList.put(name, status);
        	}
    	}
        statusList.put("summary", StatusManager.getStatusSummary(statusList));
        context.put("statusList", statusList);
    }
}
