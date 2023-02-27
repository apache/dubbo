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
package org.apache.dubbo.rpc;

import org.apache.dubbo.common.profiler.Profiler;
import org.apache.dubbo.common.profiler.ProfilerEntry;
import org.apache.dubbo.common.profiler.ProfilerSwitch;

import java.util.concurrent.Callable;

public class InvocationProfilerUtils {

    public static void enterSimpleProfiler(Invocation invocation, Callable<String> messageCallable) {
        if (ProfilerSwitch.isEnableSimpleProfiler()) {
            enterProfiler(invocation, messageCallable);
        }
    }

    public static void releaseSimpleProfiler(Invocation invocation) {
        if (ProfilerSwitch.isEnableSimpleProfiler()) {
            releaseProfiler(invocation);
        }
    }

    public static void enterDetailProfiler(Invocation invocation, Callable<String> messageCallable) {
        if (ProfilerSwitch.isEnableDetailProfiler()) {
            enterProfiler(invocation, messageCallable);
        }
    }

    public static void releaseDetailProfiler(Invocation invocation) {
        if (ProfilerSwitch.isEnableDetailProfiler()) {
            releaseProfiler(invocation);
        }
    }

    public static void enterProfiler(Invocation invocation, String message) {
        Object fromInvocation = invocation.get(Profiler.PROFILER_KEY);
        if (fromInvocation instanceof ProfilerEntry) {
            invocation.put(Profiler.PROFILER_KEY, Profiler.enter((ProfilerEntry) fromInvocation, message));
        }
    }

    public static void enterProfiler(Invocation invocation, Callable<String> messageCallable) {
        Object fromInvocation = invocation.get(Profiler.PROFILER_KEY);
        if (fromInvocation instanceof ProfilerEntry) {
            String message = "";
            try {
                message = messageCallable.call();
            } catch (Exception ignore) {

            }
            invocation.put(Profiler.PROFILER_KEY, Profiler.enter((ProfilerEntry) fromInvocation, message));
        }
    }

    public static void releaseProfiler(Invocation invocation) {
        Object fromInvocation = invocation.get(Profiler.PROFILER_KEY);
        if (fromInvocation instanceof ProfilerEntry) {
            invocation.put(Profiler.PROFILER_KEY, Profiler.release((ProfilerEntry) fromInvocation));
        }
    }
}
