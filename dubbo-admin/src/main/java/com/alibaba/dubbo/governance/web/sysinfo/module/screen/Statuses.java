/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.governance.web.sysinfo.module.screen;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.status.StatusChecker;
import com.alibaba.dubbo.governance.web.common.module.screen.Restful;
import com.alibaba.dubbo.registry.common.StatusManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class Statuses extends Restful {
    public void index(Map<String, Object> context) throws Exception {
        ExtensionLoader<StatusChecker> loader = ExtensionLoader.getExtensionLoader(StatusChecker.class);
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
