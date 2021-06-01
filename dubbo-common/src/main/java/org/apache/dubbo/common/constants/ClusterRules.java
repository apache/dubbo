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
package org.apache.dubbo.common.constants;

/**
 *  constant for Cluster fault-tolerant mode
 */
public interface ClusterRules {

    /**
     *  When invoke fails, log the initial error and retry other invokers
     *  (retry n times, which means at most n different invokers will be invoked)
     **/
    String FAIL_OVER = "failover";

    /**
     *  Execute exactly once, which means this policy will throw an exception immediately in case of an invocation error.
     **/
    String FAIL_FAST = "failfast";

    /**
     *  When invoke fails, log the error message and ignore this error by returning an empty Result.
     **/
    String FAIL_SAFE = "failsafe";

    /**
     *  When fails, record failure requests and schedule for retry on a regular interval.
     **/
    String FAIL_BACK = "failback";

    /**
     *  Invoke a specific number of invokers concurrently, usually used for demanding real-time operations, but need to waste more service resources.
     **/
    String FORKING = "forking";

    /**
     *  Call all providers by broadcast, call them one by one, and report an error if any one reports an error
     **/
    String BROADCAST = "broadcast";


    String AVAILABLE = "available";

    String MERGEABLE = "mergeable";

    String EMPTY = "";


}
