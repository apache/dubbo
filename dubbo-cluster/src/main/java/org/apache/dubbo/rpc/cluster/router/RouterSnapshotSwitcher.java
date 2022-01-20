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
package org.apache.dubbo.rpc.cluster.router;

import org.apache.dubbo.common.utils.ConcurrentHashSet;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RouterSnapshotSwitcher {
    private volatile boolean enable;
    private final Set<String> enabledService = new ConcurrentHashSet<>();

    private final static int MAX_LENGTH = 1 << 5; // 2 ^ 5 = 31

    private final AtomicInteger offset = new AtomicInteger(0);
    private volatile String[] recentSnapshot = new String[MAX_LENGTH];

    public boolean isEnable() {
        return enable;
    }

    public synchronized void addEnabledService(String service) {
        enabledService.add(service);
        enable = true;
        recentSnapshot = new String[MAX_LENGTH];
    }

    public boolean isEnable(String service) {
        return enabledService.contains(service);
    }

    public synchronized void removeEnabledService(String service) {
        enabledService.remove(service);
        enable = enabledService.size() > 0;
        recentSnapshot = new String[MAX_LENGTH];
    }

    public synchronized Set<String> getEnabledService() {
        return Collections.unmodifiableSet(enabledService);
    }

    public void setSnapshot(String snapshot) {
        if (enable) {
            // lock free
            recentSnapshot[offset.incrementAndGet() % MAX_LENGTH] = System.currentTimeMillis() + " - " + snapshot;
        }
    }

    public String[] cloneSnapshot() {
        String[] clonedSnapshot = new String[MAX_LENGTH];
        for (int i = 0; i < MAX_LENGTH; i++) {
            clonedSnapshot[i] = recentSnapshot[i];
        }
        return clonedSnapshot;
    }
}
