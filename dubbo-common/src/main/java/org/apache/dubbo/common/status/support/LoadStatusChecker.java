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
package org.apache.dubbo.common.status.support;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.status.Status;
import org.apache.dubbo.common.status.StatusChecker;
import org.apache.dubbo.common.system.OperatingSystemBeanManager;

/**
 * Load Status
 */
@Activate
public class LoadStatusChecker implements StatusChecker {

    @Override
    public Status check() {
        double load = OperatingSystemBeanManager.getOperatingSystemBean().getSystemLoadAverage();
        if (load == -1) {
            load = OperatingSystemBeanManager.getSystemCpuUsage();
        }

        int cpu = OperatingSystemBeanManager.getOperatingSystemBean().getAvailableProcessors();
        Status.Level level;
        if (load < 0) {
            level = Status.Level.UNKNOWN;
        } else if (load < cpu) {
            level = Status.Level.OK;
        } else {
            level = Status.Level.WARN;
        }

        String message = (load < 0 ? "" : "load:" + load + ",") + "cpu:" + cpu;
        return new Status(level, message);
    }
}

