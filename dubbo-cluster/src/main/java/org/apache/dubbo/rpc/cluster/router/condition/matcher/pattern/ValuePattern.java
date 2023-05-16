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
package org.apache.dubbo.rpc.cluster.router.condition.matcher.pattern;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.rpc.Invocation;

/**
 *
 */
@SPI
public interface ValuePattern {
    /**
     * Is the input pattern of a specific form, for example, range pattern '1~100', wildcard pattern 'hello*', etc.
     *
     * @param pattern the match or mismatch pattern
     * @return true or false
     */
    boolean shouldMatch(String pattern);

    /**
     * Is the pattern matches with the request context
     *
     * @param pattern         pattern value extracted from condition rule
     * @param value           the real value extracted from request context
     * @param url             request context in consumer url
     * @param invocation      request context in invocation
     * @param isWhenCondition condition type
     * @return true if successfully match
     */
    boolean match(String pattern, String value, URL url, Invocation invocation, boolean isWhenCondition);
}
