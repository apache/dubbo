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
package org.apache.dubbo.common.utils;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class TestAllowClassNotifyListener implements AllowClassNotifyListener {
    private final static AtomicReference<SerializeCheckStatus> status = new AtomicReference<>();
    private final static AtomicReference<Set<String>> allowedList = new AtomicReference<>();
    private final static AtomicReference<Set<String>> disAllowedList = new AtomicReference<>();
    private final static AtomicBoolean checkSerializable = new AtomicBoolean();

    private final static AtomicInteger count = new AtomicInteger(0);

    @Override
    public void notifyPrefix(Set<String> allowedList, Set<String> disAllowedList) {
        TestAllowClassNotifyListener.allowedList.set(allowedList);
        TestAllowClassNotifyListener.disAllowedList.set(disAllowedList);
        count.incrementAndGet();
    }

    @Override
    public void notifyCheckStatus(SerializeCheckStatus status) {
        TestAllowClassNotifyListener.status.set(status);
        count.incrementAndGet();
    }

    @Override
    public void notifyCheckSerializable(boolean checkSerializable) {
        TestAllowClassNotifyListener.checkSerializable.set(checkSerializable);
        count.incrementAndGet();
    }

    public static SerializeCheckStatus getStatus() {
        return status.get();
    }

    public static Set<String> getAllowedList() {
        return allowedList.get();
    }

    public static Set<String> getDisAllowedList() {
        return disAllowedList.get();
    }

    public static boolean isCheckSerializable() {
        return checkSerializable.get();
    }

    public static int getCount() {
        return count.get();
    }

    public static void setCount(int count) {
        TestAllowClassNotifyListener.count.set(count);
    }
}
