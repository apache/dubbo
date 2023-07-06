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

package org.apache.dubbo.metrics.model;

import org.apache.dubbo.metrics.model.MethodMetric;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * metrics data cache
 */
public class MethodMetricCache {

    private static final Map<String, MethodMetric> METHOD_METRIC_CACHE = new ConcurrentHashMap<>();


    public static MethodMetric putIfAbsent(String key, MethodMetric methodMetric) {
        return METHOD_METRIC_CACHE.putIfAbsent(key, methodMetric);
    }

    public static MethodMetric putIfAbsent(String key, ApplicationModel applicationModel, Invocation invocation) {
        return METHOD_METRIC_CACHE.putIfAbsent(key, new MethodMetric(applicationModel, invocation));
    }

    public static void remove(String key) {
        METHOD_METRIC_CACHE.remove(key);
    }

    public static boolean containsKey(String key) {
        return METHOD_METRIC_CACHE.containsKey(key);
    }


    public static MethodMetric get(String key) {
        return METHOD_METRIC_CACHE.get(key);
    }


}
