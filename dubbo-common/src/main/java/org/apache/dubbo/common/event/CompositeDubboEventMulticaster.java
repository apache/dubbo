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
package org.apache.dubbo.common.event;

import java.util.List;

public class CompositeDubboEventMulticaster implements DubboEventMulticaster {

    private final List<DubboEventMulticaster> multicasterList;

    public CompositeDubboEventMulticaster(List<DubboEventMulticaster> multicasterList) {
        this.multicasterList = multicasterList;
    }

    @Override
    public void addListener(DubboListener<?> listener) {
        for (DubboEventMulticaster dubboEventMulticaster : multicasterList) {
            dubboEventMulticaster.addListener(listener);
        }
    }

    @Override
    public void removeListener(DubboListener<?> listener) {
        for (DubboEventMulticaster dubboEventMulticaster : multicasterList) {
            dubboEventMulticaster.removeListener(listener);
        }
    }

    @Override
    public void publishEvent(DubboEvent event) {
        for (DubboEventMulticaster dubboEventMulticaster : multicasterList) {
            dubboEventMulticaster.publishEvent(event);
        }
    }
}
