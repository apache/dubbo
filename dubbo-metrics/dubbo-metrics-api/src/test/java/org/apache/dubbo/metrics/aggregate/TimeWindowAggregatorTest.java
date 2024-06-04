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
package org.apache.dubbo.metrics.aggregate;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TimeWindowAggregatorTest {
    @Test
    public void testTimeWindowAggregator() {
        TimeWindowAggregator aggregator = new TimeWindowAggregator(5, 5);

        // First time window, time range: 0 - 5 seconds

        aggregator.add(10);
        aggregator.add(20);
        aggregator.add(30);

        SampleAggregatedEntry entry1 = aggregator.get();
        Assertions.assertEquals(20, entry1.getAvg());
        Assertions.assertEquals(60, entry1.getTotal());
        Assertions.assertEquals(3, entry1.getCount());
        Assertions.assertEquals(30, entry1.getMax());
        Assertions.assertEquals(10, entry1.getMin());

        // Second time window, time range: 5 - 10 seconds
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        aggregator.add(15);
        aggregator.add(25);
        aggregator.add(35);

        SampleAggregatedEntry entry2 = aggregator.get();
        Assertions.assertEquals(25, entry2.getAvg());
        Assertions.assertEquals(75, entry2.getTotal());
        Assertions.assertEquals(3, entry2.getCount());
        Assertions.assertEquals(35, entry2.getMax());
        Assertions.assertEquals(15, entry2.getMin());

        // Third time window, time range: 10 - 15 seconds

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        aggregator.add(12);
        aggregator.add(22);
        aggregator.add(32);

        SampleAggregatedEntry entry3 = aggregator.get();
        Assertions.assertEquals(22, entry3.getAvg());
        Assertions.assertEquals(66, entry3.getTotal());
        Assertions.assertEquals(3, entry3.getCount());
        Assertions.assertEquals(32, entry3.getMax());
        Assertions.assertEquals(12, entry3.getMin());
    }
}
