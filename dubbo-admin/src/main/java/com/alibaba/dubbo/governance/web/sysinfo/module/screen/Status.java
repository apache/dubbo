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

import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.StatusManager;

/**
 * @author ding.lid
 */
public class Status extends Restful {
    public void index(Map<String, Object> context) throws Exception {
        Map<String, com.alibaba.dubbo.common.status.Status> lst = StatusManager.getInstance().getStatusList();
        Map<String, com.alibaba.dubbo.common.status.Status> statusList = new LinkedHashMap<String, com.alibaba.dubbo.common.status.Status>(lst);
        statusList.put("summary", StatusManager.getInstance().getStatusSummary(lst));
        context.put("statusList", statusList);
    }
}
