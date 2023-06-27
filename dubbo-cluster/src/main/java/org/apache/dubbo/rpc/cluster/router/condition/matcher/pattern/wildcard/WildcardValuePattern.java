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
package org.apache.dubbo.rpc.cluster.router.condition.matcher.pattern.wildcard;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.cluster.router.condition.matcher.pattern.ValuePattern;

/**
 * Matches with patterns like 'key=hello', 'key=hello*', 'key=*hello', 'key=h*o' or 'key=*'
 * <p>
 * This pattern evaluator must be the last one being executed.
 */
@Activate(order = Integer.MAX_VALUE)
public class WildcardValuePattern implements ValuePattern {
    @Override
    public boolean shouldMatch(String key) {
        return true;
    }

    @Override
    public boolean match(String pattern, String value, URL url, Invocation invocation, boolean isWhenCondition) {
        return UrlUtils.isMatchGlobPattern(pattern, value, url);
    }
}
