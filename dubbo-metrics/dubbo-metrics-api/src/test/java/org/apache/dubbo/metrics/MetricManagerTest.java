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
package org.apache.dubbo.metrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MetricManagerTest {

    @Test
    public void testNOPMetricManager() {
        Assertions.assertTrue(MetricManager.getIMetricManager() instanceof NOPMetricManager);
    }

    @Test
    public void testNOPCompass() {
        Compass compass = MetricManager.getCompass("test", MetricName.build("test"));
        compass.record(10, "success");

        Assertions.assertEquals(0, compass.getCountAndRtPerCategory().size());
        Assertions.assertEquals(0, compass.getMethodCountPerCategory().size());
        Assertions.assertEquals(0, compass.getMethodRtPerCategory().size());
    }

    @Test
    public void testNopCounter() {
        Counter counter = MetricManager.getCounter("test", MetricName.build("test2"));
        counter.inc();
        Assertions.assertEquals(0, counter.getCount());
    }

    @Test
    public void testBucketCounter() {
        BucketCounter bc = MetricManager.getBucketCounters("test", MetricName.build("test3"));
        bc.update();
        Assertions.assertEquals(0, bc.getBucketInterval());
        Assertions.assertEquals(0, bc.getBucketCounts().size());
    }
}
