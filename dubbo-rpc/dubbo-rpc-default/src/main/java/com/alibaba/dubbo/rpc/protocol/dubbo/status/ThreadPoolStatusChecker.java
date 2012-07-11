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
package com.alibaba.dubbo.rpc.protocol.dubbo.status;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.status.Status;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.common.store.DataStore;

/**
 * ThreadPoolStatusChecker
 * 
 * @author william.liangf
 */
@Activate
public class ThreadPoolStatusChecker implements StatusChecker {

    public Status check() {
        DataStore dataStore = ExtensionLoader.getExtensionLoader(DataStore.class).getDefaultExtension();
        ExecutorService executor = (ExecutorService) dataStore.get(Constants.EXECUTOR_SERVICE_COMPONENT_KEY, Constants.DEFAULT_EXECUTOR_SERVICE_KEY);
        if (executor != null && executor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tp = (ThreadPoolExecutor) executor;
            boolean ok = tp.getActiveCount() < tp.getMaximumPoolSize() - 1;
            return new Status(ok ? Status.Level.OK : Status.Level.WARN,
                              "max:" + tp.getMaximumPoolSize()
                                  + ",core:" + tp.getCorePoolSize()
                                  + ",largest:" + tp.getLargestPoolSize()
                                  + ",active:" + tp.getActiveCount()
                                  + ",task:" + tp.getTaskCount());
        }
        return new Status(Status.Level.UNKNOWN);
    }

}