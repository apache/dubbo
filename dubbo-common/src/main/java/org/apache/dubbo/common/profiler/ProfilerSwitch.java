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
package org.apache.dubbo.common.profiler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TODO
 */
public class ProfilerSwitch {
    private final static AtomicBoolean enableDetailProfiler = new AtomicBoolean(false);

    private final static AtomicBoolean enableSimpleProfiler = new AtomicBoolean(true);

    private final static AtomicReference<Double> warnPercent = new AtomicReference<>(0.75);

    public static void enableSimpleProfiler() {
        enableSimpleProfiler.set(true);
    }

    public static void disableSimpleProfiler() {
        enableSimpleProfiler.set(false);
    }

    public static void enableDetailProfiler() {
        enableDetailProfiler.set(true);
    }

    public static void disableDetailProfiler() {
        enableDetailProfiler.set(false);
    }

    public static boolean isEnableDetailProfiler() {
        return enableDetailProfiler.get() && enableSimpleProfiler.get();
    }

    public static boolean isEnableSimpleProfiler() {
        return enableSimpleProfiler.get();
    }

    public static double getWarnPercent() {
        return warnPercent.get();
    }

    public static void setWarnPercent(double percent) {
        warnPercent.set(percent);
    }
}
