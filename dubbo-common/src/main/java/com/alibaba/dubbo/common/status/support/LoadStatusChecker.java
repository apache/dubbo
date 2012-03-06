/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.status.support;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;

/**
 * Load Status
 * 
 * @author william.liangf
 */
@Activate
public class LoadStatusChecker implements StatusChecker {

    public Status check() {
    	OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    	double load;
    	try {
    	    Method method = OperatingSystemMXBean.class.getMethod("getSystemLoadAverage", new Class<?>[0]);
    	    load = (Double)method.invoke(operatingSystemMXBean, new Object[0]);
    	} catch (Throwable e) {
    	    load = -1;
    	}
    	int cpu = operatingSystemMXBean.getAvailableProcessors();
        return new Status(load < 0 ? Status.Level.UNKNOWN : (load < cpu ? Status.Level.OK : Status.Level.WARN), (load < 0 ? "" : "load:" + load + ",") + "cpu:" + cpu);
    }

}