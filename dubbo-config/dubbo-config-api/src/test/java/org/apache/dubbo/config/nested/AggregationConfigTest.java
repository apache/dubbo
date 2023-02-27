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
package org.apache.dubbo.config.nested;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class AggregationConfigTest {

    @Test
    void testEnabled() {
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setEnabled(true);
        assertThat(aggregationConfig.getEnabled(), equalTo(true));
    }

    @Test
    void testBucketNum() {
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setBucketNum(5);
        assertThat(aggregationConfig.getBucketNum(), equalTo(5));
    }

    @Test
    void testTimeWindowSeconds() {
        AggregationConfig aggregationConfig = new AggregationConfig();
        aggregationConfig.setTimeWindowSeconds(120);
        assertThat(aggregationConfig.getTimeWindowSeconds(), equalTo(120));
    }
}