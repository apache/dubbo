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
package org.apache.dubbo.config.utils;

import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.utils.service.XxxService;

import java.util.concurrent.atomic.AtomicLong;

public class XxxMockReferenceConfig extends ReferenceConfig<XxxService> {
    static AtomicLong counter = new AtomicLong();

    XxxService value;
    boolean destroyMethodRun = false;

    public static void setCounter(long c) {
        counter.set(c);
    }

    public boolean isGetMethodRun() {
        return value != null;
    }

    public boolean isDestroyMethodRun() {
        return destroyMethodRun;
    }

    @Override
    public synchronized XxxService get() {
        if (value != null) return value;

        counter.getAndIncrement();
        value = super.get();
        return value;
    }

    public long getCounter() {
        return counter.get();
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
        destroyMethodRun = true;
    }
}
