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
 *  constant for Load-balance strategy
 */
public interface LoadbalanceRules {

    /**
     *  This class select one provider from multiple providers randomly.
     **/
    String RANDOM = "random";

    /**
     * Round-robin load balance.
     **/
    String ROUND_ROBIN = "roundrobin";

    /**
     *  Filter the number of invokers with the least number of active calls and count the weights and quantities of these invokers.
     **/
    String LEAST_ACTIVE = "leastactive";

    /**
     *  Consistent Hash, requests with the same parameters are always sent to the same provider.
     **/
    String CONSISTENT_HASH = "consistenthash";

    /**
     *  Filter the number of invokers with the shortest response time of success calls and count the weights and quantities of these invokers.
     **/
    String SHORTEST_RESPONSE = "shortestresponse";

    /**
     *  adaptive load balance.
     **/
    String ADAPTIVE = "adaptive";

    String EMPTY = "";

}
