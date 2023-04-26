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
package org.apache.dubbo.rpc.cluster.router.circuitBreaker.instance;

import org.apache.dubbo.config.MethodConfig;

import java.util.concurrent.ConcurrentHashMap;

public class CircuitBreakerFactory {

    //缓存了系统的所有熔断器
    private static ConcurrentHashMap<String, CircuitBreakerInstance> circuitBreakersMap = new ConcurrentHashMap<String, CircuitBreakerInstance>();

    public static CircuitBreakerInstance getInstance(String key, MethodConfig methodConfig) {
        // this should find it for all but the first time
        //先从缓存里取
        CircuitBreakerInstance cacheCircuitBreakerInstance = circuitBreakersMap.get(key);
        if (cacheCircuitBreakerInstance != null) {
            return cacheCircuitBreakerInstance;
        }

        // if we get here this is the first time so we need to initialize

        CircuitBreakerInstance circuitBreakerInstance = circuitBreakersMap.putIfAbsent(key, new CircuitBreakerInstanceImpl<>());
        if (circuitBreakerInstance == null) {
            // this means the putIfAbsent step just created a new one so let's retrieve and return it
            return circuitBreakersMap.get(key);
        } else {
            // this means a race occurred and while attempting to 'put' another one got there before
            // and we instead retrieved it and will now return it
            return circuitBreakerInstance;
        }
    }

    static void reset() {
        circuitBreakersMap.clear();
    }
}
