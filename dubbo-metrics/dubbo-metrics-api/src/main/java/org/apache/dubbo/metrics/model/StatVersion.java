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
package org.apache.dubbo.metrics.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class StatVersion {
    private final AtomicLong version = new AtomicLong(0);
    private final List<StatVersion> child = new ArrayList<>();

    public StatVersion() {
    }

    public void increaseVersion() {
        version.incrementAndGet();
    }

    public long getVersion() {
        return version.get();
    }

    public List<StatVersion> getChild() {
        return child;
    }

    public boolean compareToChanged(StatVersion statVersion) {
        if (statVersion == null) {
            return true;
        }
        if (this.version.get() != statVersion.version.get()) {
            return true;
        }
        if (this.child.size() != statVersion.child.size()) {
            return true;
        }
        for (int i = 0; i < this.child.size(); i++) {
            if (this.child.get(i).compareToChanged(statVersion.child.get(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public StatVersion clone() {
        StatVersion statVersion = new StatVersion();
        statVersion.version.set(this.version.get());
        for (StatVersion c : this.child) {
            statVersion.child.add(c.clone());
        }
        return statVersion;
    }
}
