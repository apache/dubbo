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
package org.apache.dubbo.xds.traffic;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.TimeoutCountDown;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.TIME_COUNTDOWN_KEY;

/** A call's clock starts before routing; subsequent limits can only shorten it. */
final class XdsCallBudget {
    private static final String ATTRIBUTE = "dubbo.xds.call-budget";
    private final long started = System.nanoTime();

    static XdsCallBudget begin(URL url, Invocation invocation) {
        XdsCallBudget existing = (XdsCallBudget) invocation.get(ATTRIBUTE);
        if (existing != null) {
            return existing;
        }
        XdsCallBudget budget = new XdsCallBudget();
        long timeout = RpcUtils.getTimeout(
                url, RpcUtils.getMethodName(invocation), RpcContext.getClientAttachment(), invocation, 3000);
        TimeoutCountDown deadline =
                TimeoutCountDown.newCountDown(Math.max(0, Math.min(timeout, Integer.MAX_VALUE)), TimeUnit.MILLISECONDS);
        TimeoutCountDown inherited =
                (TimeoutCountDown) RpcContext.getClientAttachment().getObjectAttachment(TIME_COUNTDOWN_KEY);
        TimeoutCountDown attached = (TimeoutCountDown) invocation.get(TimeoutCountDown.CALL_ATTRIBUTE);
        if (inherited != null && inherited.compareTo(deadline) < 0) {
            deadline = inherited;
        }
        if (attached != null && attached.compareTo(deadline) < 0) {
            deadline = attached;
        }
        invocation.put(TimeoutCountDown.CALL_ATTRIBUTE, deadline);
        invocation.put(ATTRIBUTE, budget);
        return budget;
    }

    void limit(Invocation invocation, long maximumNanos) {
        if (maximumNanos <= 0) {
            return;
        }
        long remaining = Math.max(0, maximumNanos - (System.nanoTime() - started));
        TimeoutCountDown limit = TimeoutCountDown.newCountDown(remaining, TimeUnit.NANOSECONDS);
        TimeoutCountDown deadline = (TimeoutCountDown) invocation.get(TimeoutCountDown.CALL_ATTRIBUTE);
        if (limit.compareTo(deadline) < 0) {
            invocation.put(TimeoutCountDown.CALL_ATTRIBUTE, limit);
        }
    }
}
