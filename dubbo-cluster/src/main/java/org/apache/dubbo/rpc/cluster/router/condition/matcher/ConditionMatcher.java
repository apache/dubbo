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
package org.apache.dubbo.rpc.cluster.router.condition.matcher;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;

import java.util.Map;
import java.util.Set;

/**
 * ConditionMatcher represents a specific match condition of a condition rule.
 * <p>
 * The following condition rule '=bar&arguments[0]=hello* => region=hangzhou' consists of three ConditionMatchers:
 * 1. UrlParamConditionMatcher represented by 'foo=bar'
 * 2. ArgumentsConditionMatcher represented by 'arguments[0]=hello*'
 * 3. UrlParamConditionMatcher represented by 'region=hangzhou'
 * <p>
 * It's easy to define your own matcher by extending {@link ConditionMatcherFactory}
 */
public interface ConditionMatcher {

    /**
     * Determines if the patterns of this matcher matches with request context.
     *
     * @param sample          request context in provider url
     * @param param           request context in consumer url
     * @param invocation      request context in invocation, typically, service, method, arguments and attachments
     * @param isWhenCondition condition type
     * @return the matching result
     */
    boolean isMatch(Map<String, String> sample, URL param, Invocation invocation, boolean isWhenCondition);

    /**
     * match patterns extracted from when condition
     *
     * @return
     */
    Set<String> getMatches();

    /**
     * mismatch patterns extracted from then condition
     *
     * @return
     */
    Set<String> getMismatches();
}
