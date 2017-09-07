/**
 * Project: dubbo.registry.server-1.1.0-SNAPSHOT
 * <p>
 * File Created at 2009-12-27
 * $Id: LoadStatusChecker.java 181192 2012-06-21 05:05:47Z tony.chenl $
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
package com.alibaba.dubbo.governance.status;

import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

/**
 * Load Status
 *
 * @author william.liangf
 */
public class LoadStatusChecker implements StatusChecker {

    public Status check() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        double load;
        try {
            Method method = OperatingSystemMXBean.class.getMethod("getSystemLoadAverage", new Class<?>[0]);
            load = (Double) method.invoke(operatingSystemMXBean, new Object[0]);
        } catch (Throwable e) {
            load = -1;
        }
        int cpu = operatingSystemMXBean.getAvailableProcessors();
        return new Status(load < 0 ? Status.Level.UNKNOWN : (load < cpu ? Status.Level.OK : Status.Level.WARN), "Load: " + load + " / CPU: " + cpu);
    }

}
