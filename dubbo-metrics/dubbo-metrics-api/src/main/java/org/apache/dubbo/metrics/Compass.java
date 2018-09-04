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
 * A metric that provides an easy way to collect method invocation,
 * response time, success count, and error code count.
 */
public interface Compass extends Metric {

    /**
     * record a method invocation with execution time and sub-categories
     * @param duration must be milliseconds
     * @param subCategory all the sub-categories should be orthogonal,
     *                    which will be added up to the total number of method invocations
     */
    void record(long duration, String subCategory);
    
    /**
     * return method count per bucket per category
     * @return
     */
    Map<String, Map<Long, Long>> getMethodCountPerCategory();

    /**
     * return method count per bucket per category
     * @return
     */
    Map<String, Map<Long, Long>> getMethodCountPerCategory(long startTime);

    /**
     * return method execution time per bucket per category
     * @return
     */
    Map<String, Map<Long, Long>> getMethodRtPerCategory();

    /**
     * return method execution time per bucket per category
     * @return
     */
    Map<String, Map<Long, Long>> getMethodRtPerCategory(long startTime);

    /**
     * return method execution time and count per bucket per category
     * @return
     */
    Map<String, Map<Long, Long>> getCountAndRtPerCategory();
    
    /**
     * return method execution time and count per bucket per category
     * @return
     */
    Map<String, Map<Long, Long>> getCountAndRtPerCategory(long startTime);
    
    /**
     * @return the bucket interval
     */
    int getBucketInterval();
}
