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
package com.alibaba.dubbo.config.utils;

import com.alibaba.dubbo.config.ReferenceConfig;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ding.lid
 */
public class MockReferenceConfig extends ReferenceConfig<String> {
    static AtomicLong counter = new AtomicLong();

    String value;

    public boolean isGetMethodRun() {
        return value != null;
    }

    boolean destroyMethodRun = false;

    public boolean isDestroyMethodRun() {
        return destroyMethodRun;
    }

    public static void setCounter(long c) {
        counter.set(c);
    }

    @Override
    public synchronized String get() {
        if(value != null) return value;

        value = "" + counter.getAndIncrement();
        return value;
    }

    @Override
    public synchronized void destroy() {
        destroyMethodRun = true;
    }
}
