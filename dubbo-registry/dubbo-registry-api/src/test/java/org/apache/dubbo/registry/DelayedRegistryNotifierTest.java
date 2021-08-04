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
package org.apache.dubbo.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DelayedRegistryNotifierTest {
    @Test
    public void testDelay() throws InterruptedException {
        MockRegistryNotifier notifier = new MockRegistryNotifier(10000);

        // The first time the notification is triggered immediately, let lastExecuteTime have a value instead of the
        // default value of 0
        notifier.notify("dubbo://127.0.0.1");
        // Take a nap to make sure the scheduler thread finishes the task
        Thread.sleep(500);
        Assertions.assertEquals(notifier.getCount(), 1);

        // Generate 10 delayed tasks, the latest value of lastEventTime is the last call, so that only the last delayed
        // task satisfies condition that "this.time == listener.lastEventTime", and execute the "doNotify" logic
        for (int i = 0; i < 10; i++) {
            Thread.sleep(500);
            notifier.notify("dubbo://127.0.0.1");
        }
        Thread.sleep(10000);
        Assertions.assertEquals(notifier.getCount(), 2);
    }
}

class MockRegistryNotifier extends RegistryNotifier {

    private int count = 0;

    public MockRegistryNotifier(long delayTime) {
        super(delayTime);
    }

    @Override
    protected void doNotify(Object rawAddresses) {
        count++;
    }

    public int getCount() {
        return count;
    }
}
