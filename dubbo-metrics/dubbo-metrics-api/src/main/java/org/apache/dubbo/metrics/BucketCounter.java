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

import java.util.Map;

/**
 * Store the count in multiple buckets,
 * every event will go into one specific bucket according to the happening timestamp.
 * The BucketCounter will reserve data for the last N time interval,
 * older values will be automatically discarded.
 */
public interface BucketCounter extends Metric {

    /**
     * update the counter to the given bucket
     */
    void update();

    /**
     * update the counter to the given bucket
     */
    void update(long n);

    /**
     * Return the bucket count, keyed by timestamp
     * @return the bucket count, keyed by timestamp
     */
    Map<Long, Long> getBucketCounts();

    /**
     * Return the bucket count, keyed by timestamp, since (including) the startTime.
     * @param startTime the start time
     * @return the bucket count, keyed by timestamp
     */
    Map<Long, Long> getBucketCounts(long startTime);

    /**
     * Get the interval of the bucket
     * @return the interval of the bucket
     */
    int getBucketInterval();
}
